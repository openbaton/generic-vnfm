/*
 * Copyright (c) 2016 Open Baton (http://www.openbaton.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.openbaton.vnfm.generic;

import com.google.gson.JsonObject;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import org.apache.commons.codec.binary.Base64;
import org.openbaton.catalogue.mano.common.Event;
import org.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.Action;
import org.openbaton.catalogue.nfvo.ConfigurationParameter;
import org.openbaton.catalogue.nfvo.DependencyParameters;
import org.openbaton.catalogue.nfvo.Script;
import org.openbaton.catalogue.nfvo.viminstances.BaseVimInstance;
import org.openbaton.common.vnfm_sdk.AbstractVnfm;
import org.openbaton.common.vnfm_sdk.amqp.AbstractVnfmSpringAmqp;
import org.openbaton.common.vnfm_sdk.exception.BadFormatException;
import org.openbaton.common.vnfm_sdk.exception.VnfmSdkException;
import org.openbaton.common.vnfm_sdk.utils.VnfmUtils;
import org.openbaton.vnfm.generic.configuration.EMSConfiguration;
import org.openbaton.vnfm.generic.core.ElementManagementSystem;
import org.openbaton.vnfm.generic.core.LifeCycleManagement;
import org.openbaton.vnfm.generic.model.EmsRegistrationUnit;
import org.openbaton.vnfm.generic.model.VNFRErrorStatus;
import org.openbaton.vnfm.generic.repository.VNFRErrorRepository;
import org.openbaton.vnfm.generic.utils.JsonUtils;
import org.openbaton.vnfm.generic.utils.LogDispatcher;
import org.openbaton.vnfm.generic.utils.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ConfigurationProperties
@SpringBootApplication
@EnableJpaRepositories("org.openbaton.vnfm.generic.repository")
public class GenericVNFM extends AbstractVnfmSpringAmqp {

  @Autowired private ElementManagementSystem ems;

  @Value("${vnfm.ems.username:admin}")
  private String emsRabbitUsername;

  @Value("${vnfm.ems.password:openbaton}")
  private String emsRabbitPassword;

  @Value("${vnfm.endpoint:generic}")
  private String endpointProp;

  @Value("${vnfm.type:generic}")
  private String type;

  @Value("${vnfm.ems.offline: false}")
  private boolean emsOffline;

  @Autowired private LifeCycleManagement lcm;

  @Value("${vnfm.ems.userdata.filepath:/etc/openbaton/openbatonmo-vnfm-generic-user-data.sh}")
  private String userDataFilePath;

  @Autowired private LogUtils logUtils;

  @Autowired private LogDispatcher logDispatcher;
  @Autowired private EMSConfiguration emsConfiguration;

  @Autowired private VNFRErrorRepository vnfrErrorRepository;

  public static void main(String[] args) {
    SpringApplication.run(GenericVNFM.class, args);
  }

  private static String convertStreamToString(InputStream is) {
    Scanner s = new Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  @Override
  public VirtualNetworkFunctionRecord instantiate(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      Object scripts,
      Map<String, Collection<BaseVimInstance>> vimInstances)
      throws Exception {

    log.info(
        "Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());

    Set<EmsRegistrationUnit> registrationUnits = new HashSet<>();
    for (VirtualDeploymentUnit virtualDeploymentUnit : virtualNetworkFunctionRecord.getVdu()) {
      for (VNFCInstance vnfcInstance : virtualDeploymentUnit.getVnfc_instance()) {
        registrationUnits.add(ems.addRegistrationUnit(vnfcInstance.getHostname()));
      }
    }
    Set<Future<EmsRegistrationUnit>> waiters = new HashSet<>();
    registrationUnits.forEach(
        u -> {
          log.debug(String.format("Waiting for EMS: %s", u.getValue()));
          waiters.add(ems.waitForEms(() -> u.waitForEms(emsConfiguration.getWaitForEms() * 1000)));
        });

    for (Future<EmsRegistrationUnit> unitFuture : waiters) {
      EmsRegistrationUnit unit = unitFuture.get();
      ems.removeRegistrationUnit(unit);
      if (!unit.isRegistered()) {
        log.error(String.format("Timeout waiting for EMS: %s", unit.getValue()));
        throw new VnfmSdkException(String.format("Timeout waiting for EMS: %s", unit.getValue()));
      }
      if (unit.isCanceled()) {
        log.info(String.format("Cancelled: %s", unit.getValue()));
        return null;
      } else {
        log.info(String.format("Registered EMS: %s", unit.getValue()));
      }
    }
    if (null != scripts) {
      ems.saveScriptOnEms(virtualNetworkFunctionRecord, scripts);
    }

    for (VirtualDeploymentUnit virtualDeploymentUnit : virtualNetworkFunctionRecord.getVdu()) {
      for (VNFCInstance vnfcInstance : virtualDeploymentUnit.getVnfc_instance()) {
        log.debug("VNFCInstance: " + vnfcInstance);
      }
    }

    StringBuilder output = new StringBuilder("\n--------------------\n");
    for (String result :
        lcm.executeScriptsForEvent(virtualNetworkFunctionRecord, Event.INSTANTIATE)) {
      output.append(JsonUtils.parse(result));
      output.append("\n--------------------\n");
    }

    log.info("Executed script for INSTANTIATE. Output was: \n\n" + output);
    return virtualNetworkFunctionRecord;
  }

  @Override
  public void query() {}

  @Override
  public VirtualNetworkFunctionRecord scale(
      Action scaleInOrOut,
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      VNFComponent component,
      Object scripts,
      VNFRecordDependency dependency)
      throws Exception {
    VNFCInstance vnfcInstance = (VNFCInstance) component;
    if (scaleInOrOut.ordinal() == Action.SCALE_OUT.ordinal()) {
      log.info("Created VNFComponent");
      EmsRegistrationUnit registrationUnit = ems.addRegistrationUnit(vnfcInstance.getHostname());
      EmsRegistrationUnit finalRegistrationUnit = registrationUnit;
      registrationUnit =
          ems.waitForEms(
                  () -> finalRegistrationUnit.waitForEms(emsConfiguration.getWaitForEms() * 1000))
              .get();
      if (registrationUnit.isCanceled()) return null;
      else log.info(String.format("Registered EMS: %s", registrationUnit.getValue()));
      ems.saveScriptOnEms(vnfcInstance, scripts, virtualNetworkFunctionRecord);
      StringBuilder output = new StringBuilder("\n--------------------\n--------------------\n");
      for (String result :
          lcm.executeScriptsForEvent(
              virtualNetworkFunctionRecord, vnfcInstance, Event.INSTANTIATE)) {
        output.append(JsonUtils.parse(result));
        output.append("\n--------------------\n");
      }
      output.append("\n--------------------\n");
      log.info("Executed script for INSTANTIATE. Output was: \n\n" + output);

      if (dependency != null) {
        output = new StringBuilder("\n--------------------\n--------------------\n");
        for (String result :
            lcm.executeScriptsForEvent(
                virtualNetworkFunctionRecord, vnfcInstance, Event.CONFIGURE, dependency)) {
          output.append(JsonUtils.parse(result));
          output.append("\n--------------------\n");
        }
        output.append("\n--------------------\n");
        log.info("Executed script for CONFIGURE. Output was: \n\n" + output);
      }

      if ((vnfcInstance.getState() == null) || !vnfcInstance.getState().equals("STANDBY")) {
        if (VnfmUtils.getLifecycleEvent(
                virtualNetworkFunctionRecord.getLifecycle_event(), Event.START)
            != null) {
          output = new StringBuilder("\n--------------------\n--------------------\n");
          for (String result :
              lcm.executeScriptsForEvent(virtualNetworkFunctionRecord, vnfcInstance, Event.START)) {
            output.append(JsonUtils.parse(result));
            output.append("\n--------------------\n");
          }
          output.append("\n--------------------\n");
          log.info("Executed script for START. Output was: \n\n" + output);
        }
      }

      log.trace("HB_VERSION == " + virtualNetworkFunctionRecord.getHbVersion());
      return virtualNetworkFunctionRecord;
    } else { // SCALE_IN

      StringBuilder output = new StringBuilder("\n--------------------\n--------------------\n");
      for (String result :
          lcm.executeScriptsForEventOnVnfr(
              virtualNetworkFunctionRecord, vnfcInstance, Event.SCALE_IN)) {
        output.append(JsonUtils.parse(result));
        output.append("\n--------------------\n");
      }
      output.append("\n--------------------\n");
      log.info("Executed script for SCALE_IN. Output was: \n\n" + output);

      return virtualNetworkFunctionRecord;
    }
  }

  @Override
  public void checkInstantiationFeasibility() {}

  @Override
  public VirtualNetworkFunctionRecord heal(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      VNFCInstance vnfcInstanceSample,
      String cause)
      throws Exception {

    if (cause.equals("switchToStandby")) {
      // execute start in the standby vnfc instance and set state to ACTIVE
      VNFCInstance vnfcInstance =
          getVNFCInstanceByVNFComponentId(virtualNetworkFunctionRecord, vnfcInstanceSample.getId());
      if (vnfcInstance != null && vnfcInstance.getState().equalsIgnoreCase("standby")) {
        log.debug("Activation of the standby VNFCInstance");
        boolean isStartLifeCycleEventPresent =
            VnfmUtils.getLifecycleEvent(
                    virtualNetworkFunctionRecord.getLifecycle_event(), Event.START)
                != null;
        if (isStartLifeCycleEventPresent) {
          log.debug(
              "Executed scripts for event START "
                  + lcm.executeScriptsForEvent(
                      virtualNetworkFunctionRecord, vnfcInstance, Event.START));
          //This is inside the vnfr
          vnfcInstance.setState("ACTIVE");
          // This is a copy of the object received as parameter and modified.
          // It will be sent to the orchestrator
          vnfcInstanceSample.setState("ACTIVE");
        }
      } else log.warn("Not found VNFC instance in standby with id: " + vnfcInstanceSample.getId());
    } else if (VnfmUtils.getLifecycleEvent(
            virtualNetworkFunctionRecord.getLifecycle_event(), Event.HEAL)
        != null) {
      log.debug("Heal method started");
      log.info("-----------------------------------------------------------------------");
      StringBuilder output = new StringBuilder("\n--------------------\n--------------------\n");
      for (String result :
          lcm.executeScriptsForEvent(
              virtualNetworkFunctionRecord, vnfcInstanceSample, Event.HEAL, cause)) {
        output.append(JsonUtils.parse(result));
        output.append("\n--------------------\n");
      }
      output.append("\n--------------------\n");
      log.info("Executed script for HEAL. Output was: \n\n" + output);
      log.info("-----------------------------------------------------------------------");
    }
    return virtualNetworkFunctionRecord;
  }

  private VNFCInstance getVNFCInstanceByVNFComponentId(
      VirtualNetworkFunctionRecord vnfr, String vnfComponentId) {
    for (VirtualDeploymentUnit virtualDeploymentUnit : vnfr.getVdu())
      for (VNFCInstance vnfcInstance : virtualDeploymentUnit.getVnfc_instance())
        if (vnfcInstance.getId().equals(vnfComponentId)) return vnfcInstance;
    return null;
  }

  @Override
  public VirtualNetworkFunctionRecord updateSoftware(
      Script script, VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {
    /*
     * If script is null the "updateSoftware" operation is intended as the execution of the UPDATE lifecycle event
     * otherwise the content of the script passed is used to update the correspondent script in the VNFC instances
     *
     * */
    if (script == null) lcm.executeScriptsForEvent(virtualNetworkFunctionRecord, Event.UPDATE);
    else {
      for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
        for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
          updateScript(script, virtualNetworkFunctionRecord, vnfcInstance);
        }
      }
    }
    return virtualNetworkFunctionRecord;
  }

  private void updateScript(
      Script script,
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      VNFCInstance vnfcInstance)
      throws Exception {
    JsonObject jsonMessage =
        JsonUtils.getJsonObjectForScript(
            "SCRIPTS_UPDATE",
            Base64.encodeBase64String(script.getPayload()),
            script.getName(),
            properties.getProperty("script-path", "/opt/openbaton/scripts"));
    ems.executeActionOnEMS(
        vnfcInstance.getHostname(),
        jsonMessage.toString(),
        virtualNetworkFunctionRecord,
        vnfcInstance);
  }

  @Override
  public void upgradeSoftware() {}

  @Override
  public VirtualNetworkFunctionRecord modify(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFRecordDependency dependency)
      throws Exception {
    log.trace(
        "VirtualNetworkFunctionRecord VERSION is: " + virtualNetworkFunctionRecord.getHbVersion());
    log.info("executing modify for VNFR: " + virtualNetworkFunctionRecord.getName());

    log.debug("Got dependency: " + dependency);
    log.debug("Parameters are: ");
    for (Entry<String, DependencyParameters> entry : dependency.getParameters().entrySet()) {
      log.debug("Source type: " + entry.getKey());
      log.debug("Parameters: " + entry.getValue().getParameters());
    }

    if (VnfmUtils.getLifecycleEvent(
            virtualNetworkFunctionRecord.getLifecycle_event(), Event.CONFIGURE)
        != null) {
      log.debug(
          "LifeCycle events: "
              + VnfmUtils.getLifecycleEvent(
                      virtualNetworkFunctionRecord.getLifecycle_event(), Event.CONFIGURE)
                  .getLifecycle_events());
      log.info("-----------------------------------------------------------------------");
      StringBuilder output = new StringBuilder("\n--------------------\n--------------------\n");
      for (String result :
          lcm.executeScriptsForEvent(virtualNetworkFunctionRecord, Event.CONFIGURE, dependency)) {
        output.append(JsonUtils.parse(result));
        output.append("\n--------------------\n");
      }
      output.append("\n--------------------\n");
      log.info("Executed script for CONFIGURE. Output was: \n\n" + output);
      log.info("-----------------------------------------------------------------------");
    } else {
      log.debug("No LifeCycle events for Event.CONFIGURE");
    }
    return virtualNetworkFunctionRecord;
  }

  //When the EMS receives a script which terminate the vnf, the EMS is still running.
  //Once the vnf is terminated NFVO requests deletion of resources (MANO B.5) and the EMS will be terminated.
  @Override
  public VirtualNetworkFunctionRecord terminate(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {
    log.debug("Termination of VNF: " + virtualNetworkFunctionRecord.getName());
    if (VnfmUtils.getLifecycleEvent(
            virtualNetworkFunctionRecord.getLifecycle_event(), Event.TERMINATE)
        != null) {
      StringBuilder output = new StringBuilder("\n--------------------\n--------------------\n");
      for (String result :
          lcm.executeScriptsForEvent(virtualNetworkFunctionRecord, Event.TERMINATE)) {
        output.append(JsonUtils.parse(result));
        output.append("\n--------------------\n");
      }
      output.append("\n--------------------\n");
      log.info("Executed script for TERMINATE. Output was: \n\n" + output);
    }

    for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
      for (VNFCInstance vnfci : vdu.getVnfc_instance()) {
        try {
          ems.removeRegistrationUnit(vnfci.getHostname());
          if (vnfrErrorRepository.findFirstByVnfrId(virtualNetworkFunctionRecord.getId()) != null) {
            vnfrErrorRepository.deleteByVnfrId(virtualNetworkFunctionRecord.getId());
            log.info(
                "Error information for terminated VNRF with id: "
                    + virtualNetworkFunctionRecord.getId()
                    + "  deleted from database");
          }
        } catch (BadFormatException e) {
          e.printStackTrace();
        }
      }
    }

    return virtualNetworkFunctionRecord;
  }

  @Override
  public void handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
    log.error("Received Error for VNFR " + virtualNetworkFunctionRecord.getName());
    if (VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.ERROR)
        != null) {
      StringBuilder output = new StringBuilder("\n--------------------\n--------------------\n");
      try {
        for (String result :
            lcm.executeScriptsForEvent(virtualNetworkFunctionRecord, Event.ERROR)) {
          output.append(JsonUtils.parse(result));
          output.append("\n--------------------\n");
        }
      } catch (Exception e) {
        e.printStackTrace();
        log.error("Exception executing Error handling");
      }
      output.append("\n--------------------\n");
      log.info("Executed script for ERROR. Output was: \n\n" + output);
    }
  }

  @Override
  protected void fillSpecificProvides(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
    for (ConfigurationParameter configurationParameter :
        virtualNetworkFunctionRecord.getProvides().getConfigurationParameters()) {
      if (!configurationParameter.getConfKey().startsWith("#nfvo:")) {
        configurationParameter.setValue(String.valueOf((int) (Math.random() * 100)));
        log.debug(
            "Setting: "
                + configurationParameter.getConfKey()
                + " with value: "
                + configurationParameter.getValue());
      }
    }
  }

  @Override
  public VirtualNetworkFunctionRecord start(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {

    log.info("Starting vnfr: " + virtualNetworkFunctionRecord.getName());

    if (VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.START)
        != null) {
      if (VnfmUtils.getLifecycleEvent(
                  virtualNetworkFunctionRecord.getLifecycle_event(), Event.START)
              .getLifecycle_events()
          != null) {
        StringBuilder output = new StringBuilder("\n--------------------\n--------------------\n");
        for (String result :
            lcm.executeScriptsForEvent(virtualNetworkFunctionRecord, Event.START)) {
          output.append(JsonUtils.parse(result));
          output.append("\n--------------------\n");
        }
        output.append("\n--------------------\n");
        log.info("Executed script for START. Output was: \n\n" + output);
      }
    }
    return virtualNetworkFunctionRecord;
  }

  @Override
  public VirtualNetworkFunctionRecord stop(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {

    log.info("Stopping vnfr: " + virtualNetworkFunctionRecord.getName());

    if (VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.STOP)
        != null) {
      if (VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.STOP)
              .getLifecycle_events()
          != null) {
        StringBuilder output = new StringBuilder("\n--------------------\n--------------------\n");
        for (String result : lcm.executeScriptsForEvent(virtualNetworkFunctionRecord, Event.STOP)) {
          output.append(JsonUtils.parse(result));
          output.append("\n--------------------\n");
        }
        output.append("\n--------------------\n");
        log.info("Executed script for STOP. Output was: \n\n" + output);
      }
    }
    return virtualNetworkFunctionRecord;
  }

  @Override
  public VirtualNetworkFunctionRecord startVNFCInstance(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance vnfcInstance)
      throws Exception {

    log.info("Starting vnfc instance: " + vnfcInstance.getHostname());

    if (VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.START)
        != null) {
      if (VnfmUtils.getLifecycleEvent(
                  virtualNetworkFunctionRecord.getLifecycle_event(), Event.START)
              .getLifecycle_events()
          != null) {
        StringBuilder output = new StringBuilder("\n--------------------\n--------------------\n");
        for (String result :
            lcm.executeScriptsForEvent(virtualNetworkFunctionRecord, vnfcInstance, Event.START)) {
          output.append(JsonUtils.parse(result));
          output.append("\n--------------------\n");
        }
        output.append("\n--------------------\n");
        log.info(
            "Executed script for START on VNFC Instance "
                + vnfcInstance.getHostname()
                + ". Output was: \n\n"
                + output);
      }
    }

    return virtualNetworkFunctionRecord;
  }

  @Override
  public VirtualNetworkFunctionRecord stopVNFCInstance(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance vnfcInstance)
      throws Exception {

    log.info("Stopping vnfc instance: " + vnfcInstance.getHostname());

    if (VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.STOP)
        != null) {
      if (VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.STOP)
              .getLifecycle_events()
          != null) {
        StringBuilder output = new StringBuilder("\n--------------------\n--------------------\n");
        for (String result :
            lcm.executeScriptsForEvent(virtualNetworkFunctionRecord, vnfcInstance, Event.STOP)) {
          output.append(JsonUtils.parse(result));
          output.append("\n--------------------\n");
        }
        output.append("\n--------------------\n");
        log.info(
            "Executed script for STOP on VNFC Instance "
                + vnfcInstance.getHostname()
                + ". Output was: \n\n"
                + output);
      }
    }

    return virtualNetworkFunctionRecord;
  }

  @Override
  public VirtualNetworkFunctionRecord configure(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
    return virtualNetworkFunctionRecord;
  }

  @Override
  public VirtualNetworkFunctionRecord resume(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      VNFCInstance vnfcInstance,
      VNFRecordDependency dependency)
      throws Exception {
    log.info("Resuming VNFR: " + virtualNetworkFunctionRecord.getId());
    VNFRErrorStatus vnfrErrorStatus =
        vnfrErrorRepository.findFirstByVnfrId(virtualNetworkFunctionRecord.getId());
    if (vnfrErrorStatus != null) {
      try {
        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
          for (VNFCInstance vnfci : vdu.getVnfc_instance()) {
            log.info(
                "Found Vnfci: "
                    + vnfci.getHostname()
                    + " , Event: "
                    + vnfrErrorStatus.getEvent().name()
                    + " and Script# "
                    + vnfrErrorStatus.getScriptIndex());
            log.info("-----------------------------------------------------------------------");
            StringBuilder output =
                new StringBuilder("\n--------------------\n--------------------\n");
            for (String result :
                lcm.executeScriptsForEvent(
                    virtualNetworkFunctionRecord,
                    vnfrErrorStatus.getEvent(),
                    vnfci,
                    vnfrErrorStatus.getScriptIndex(),
                    dependency)) {
              output.append(JsonUtils.parse(result));
              output.append("\n--------------------\n");
            }
            output.append("\n--------------------\n");
            log.info("Executed script for RESUME. Output was: \n\n" + output);
            log.info("-----------------------------------------------------------------------");
          }
        }
        //      delete vnfrError record
        log.debug(
            "Deleting error information from database for VNFR: "
                + virtualNetworkFunctionRecord.getId());
        if (virtualNetworkFunctionRecord != null)
          vnfrErrorRepository.deleteByVnfrId(virtualNetworkFunctionRecord.getId());
      } catch (Exception e) {
        throw new VnfmSdkException(
            "VNFR: "
                + virtualNetworkFunctionRecord.getId()
                + " has thrown the following error while resume action: "
                + e);
      }
    }
    return virtualNetworkFunctionRecord;
  }

  @Override
  public VirtualNetworkFunctionRecord executeScript(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Script script) throws Exception {
    for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
      for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
        StringBuilder output = new StringBuilder("\n--------------------\n--------------------");
        log.info(output.toString());
        log.info(
            "Executing script '"
                + script.getName()
                + "' on VNFCI '"
                + vnfcInstance.getHostname()
                + "' :");
        log.info(script.toString());

        // Saving script on VNFC instance
        Set<Script> scriptSet = new HashSet<>();
        scriptSet.add(script);
        ems.saveScriptOnEms(virtualNetworkFunctionRecord, scriptSet);

        // Executing script on VNFC instance
        JsonObject jsonMessage =
            JsonUtils.getJsonObject(
                "EXECUTE",
                script.getName(),
                properties.getProperty("script-path", "/opt/openbaton/scripts"));
        ems.executeActionOnEMS(
            vnfcInstance.getHostname(),
            jsonMessage.toString(),
            virtualNetworkFunctionRecord,
            vnfcInstance);

        log.info(output.toString());
      }
    }
    return virtualNetworkFunctionRecord;
  }

  @Override
  protected Action getResumedAction(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance vnfcInstance)
      throws Exception {
    try {
      Event failedEvent =
          vnfrErrorRepository.findFirstByVnfrId(virtualNetworkFunctionRecord.getId()).getEvent();
      return getActionFromEvent(failedEvent);
    } catch (Exception e) {
      throw new VnfmSdkException(
          "Resume is allowed only for VNFRs with script errors. Please check log for the previous error information.");
    }
  }

  private Action getActionFromEvent(Event event) {
    Action action = null;
    switch (event) {
      case INSTANTIATE:
        action = Action.INSTANTIATE;
        break;
      case CONFIGURE:
        action = Action.MODIFY;
        break;
      case STOP:
        action = Action.STOP;
        break;
      case START:
        action = Action.START;
        break;
      case TERMINATE:
        action = Action.RELEASE_RESOURCES;
        break;
      case ERROR:
        action = Action.ERROR;
        break;
      case HEAL:
        action = Action.HEAL;
        break;
      case UPDATE:
        action = Action.UPDATE;
        break;
    }
    return action;
  }

  @Override
  public void NotifyChange() {}

  @Override
  protected void setup() {
    super.setup();
    String scriptPath = properties.getProperty("script-path", "/opt/openbaton/scripts");
    logUtils.init();
    ems.init(scriptPath, vnfmHelper);
    super.logDispatcher = this.logDispatcher;
  }

  @Override
  protected String getUserData() {
    String result;
    try {
      log.info("Attempting Userdata file (from properties file): " + userDataFilePath);
      result = convertStreamToString(new FileInputStream(userDataFilePath));
    } catch (FileNotFoundException e) {
      log.warn("Userdata file not found: " + userDataFilePath);
      try {
        log.warn("Attempting Userdata file (from classpath): /user-data.sh");
        result = convertStreamToString(AbstractVnfm.class.getResourceAsStream("/user-data.sh"));
      } catch (NullPointerException npe) {
        log.error("Userdata file not found: '/user-data.sh'");
        throw npe;
      }
    }

    log.debug(ems.getEmsVersion());
    if (emsOffline) {
      result = result.replace("export OFFLINE_EMS=", "export OFFLINE_EMS=1");
    } else {
      result = result.replace("export OFFLINE_EMS=", "export OFFLINE_EMS=0");
    }
    result = result.replace("export MONITORING_IP=", "export MONITORING_IP=" + monitoringIp);
    result = result.replace("export TIMEZONE=", "export TIMEZONE=" + timezone);
    result = result.replace("export BROKER_IP=", "export BROKER_IP=" + brokerIp);
    result = result.replace("export BROKER_PORT=", "export BROKER_PORT=" + brokerPort);
    result = result.replace("export USERNAME=", "export USERNAME=" + emsRabbitUsername);
    result = result.replace("export PASSWORD=", "export PASSWORD=" + emsRabbitPassword);
    result =
        result.replace("export EXCHANGE_NAME=", "export EXCHANGE_NAME=" + "openbaton-exchange");
    result =
        result.replace("export EMS_HEARTBEAT=", "export EMS_HEARTBEAT=" + ems.getEmsHeartbeat());
    result =
        result.replace("export EMS_AUTODELETE=", "export EMS_AUTODELETE=" + ems.getEmsAutodelete());
    result = result.replace("export EMS_VERSION=", "export EMS_VERSION=" + ems.getEmsVersion());
    result = result.replace("export ENDPOINT=", "export ENDPOINT=" + this.endpointProp);

    return result;
  }
}
