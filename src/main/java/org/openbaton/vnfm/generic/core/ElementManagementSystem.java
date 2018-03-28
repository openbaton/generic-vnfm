/*
 *
 *  * Copyright (c) 2016 Fraunhofer FOKUS
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package org.openbaton.vnfm.generic.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Base64;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.Script;
import org.openbaton.common.vnfm_sdk.VnfmHelper;
import org.openbaton.common.vnfm_sdk.exception.BadFormatException;
import org.openbaton.vnfm.generic.configuration.EMSConfiguration;
import org.openbaton.vnfm.generic.interfaces.EmsInterface;
import org.openbaton.vnfm.generic.model.EmsRegistrationUnit;
import org.openbaton.vnfm.generic.utils.JsonUtils;
import org.openbaton.vnfm.generic.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class ElementManagementSystem implements EmsInterface {

  private static Gson parser = new GsonBuilder().setPrettyPrinting().create();

  private Logger log = LoggerFactory.getLogger(getClass());

  @Autowired private EMSConfiguration emsConfiguration;

  //TODO consider using DB in case of failure etc...
  private static Set<EmsRegistrationUnit> registrationUnits;
  private ThreadPoolExecutor executor;

  private String scriptPath;
  // Path of the parameters' file on the EMS
  // If it is /opt/openbaton/, in such path will be stored the VNFRDependency object
  private String parametersFilePath;

  private VnfmHelper vnfmHelper;
  @Autowired private LogUtils logUtils;

  public void init(String scriptPath, VnfmHelper vnfmHelper) {
    this.scriptPath = scriptPath;
    this.parametersFilePath = this.scriptPath;
    registrationUnits = ConcurrentHashMap.newKeySet();
    executor =
        new ThreadPoolExecutor(5, 10, 5000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    this.vnfmHelper = vnfmHelper;
  }

  @Override
  public synchronized EmsRegistrationUnit addRegistrationUnit(String hostname, boolean registered) {
    this.log.debug(
        String.format(
            "EMSRegister adding: %s with registered status (%s) to list:\n%s",
            hostname, registered, registrationUnits));
    Optional<EmsRegistrationUnit> unit =
        registrationUnits
            .stream()
            .filter(
                h -> {
                  try {
                    return h.getValue().contains(extractIdFromHostname(hostname));
                  } catch (BadFormatException e) {
                    e.printStackTrace();
                    return false;
                  }
                })
            .findAny();
    if (!unit.isPresent()) {
      log.debug(
          String.format(
              "Adding for first time unit %s that was not found in registration unit list:\n%s",
              unit, registrationUnits));
      EmsRegistrationUnit registrationUnit = new EmsRegistrationUnit();
      registrationUnit.setRegistered(registered);
      registrationUnit.setCanceled(false);
      registrationUnit.setValue(hostname);
      registrationUnits.add(registrationUnit);
      return registrationUnit;
    } else {
      log.debug(
          String.format("Found unit %s in registration unit list:\n%s", unit, registrationUnits));
      return unit.get();
    }
  }

  @Override
  public synchronized EmsRegistrationUnit addRegistrationUnit(String hostname) {
    log.debug(
        String.format(
            "Calling add registration unit with hostname: %s with registered set to false",
            hostname));
    return addRegistrationUnit(hostname, false);
  }

  @Override
  public synchronized void removeRegistrationUnit(String hostname) throws BadFormatException {
    this.log.debug("EMSRegister removing: " + hostname);
    String extractedId = extractIdFromHostname(hostname);
    registrationUnits =
        registrationUnits
            .stream()
            .filter(
                h -> {
                  if (h.getValue().contains(extractedId)) {
                    h.cancelAndNotify();
                  }
                  return !h.getValue().contains(extractedId);
                })
            .collect(Collectors.toSet());
  }

  @Override
  public void registerFromEms(String json) {
    this.log.debug("EMSRegister received from EMS: " + json);
    JsonObject object = parser.fromJson(json, JsonObject.class);
    String hostname = object.get("hostname").getAsString();
    String extractedId = "";
    try {
      extractedId = extractIdFromHostname(hostname);
    } catch (BadFormatException e) {
      log.info(e.getMessage());
      return;
    }
    EmsRegistrationUnit u = addRegistrationUnit(hostname);
    u.registerAndNotify();
    //    for (EmsRegistrationUnit registrationUnit : registrationUnits) {
    //      if (registrationUnit.getValue().endsWith(extractedId)) {
    //        log.debug(String.format("Notify to %s", registrationUnit.getValue()));
    //        registrationUnit.registerAndNotify();
    //        return;
    //      }
    //    }
  }

  public Future<EmsRegistrationUnit> waitForEms(
      Callable<EmsRegistrationUnit> registrationUnitCallable) {

    return executor.submit(registrationUnitCallable);
  }

  private String extractIdFromHostname(String hostname) throws BadFormatException {
    String extractedId;
    Pattern pattern = Pattern.compile("-(\\d+)$");
    Matcher matcher = pattern.matcher(hostname.trim());
    if (matcher.find()) {
      extractedId = (matcher.group(1));
    } else {
      throw new BadFormatException(
          "Hostname does not fit the expected format. Must fit: '.*-[1-9]+$'");
    }
    return extractedId;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void saveScriptOnEms(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Object scripts) throws Exception {

    log.debug("Scripts are: " + scripts.getClass().getName());

    if (scripts instanceof String) {
      String scriptLink = (String) scripts;
      log.debug("Scripts are: " + scriptLink);
      JsonObject jsonMessage = JsonUtils.getJsonObject("CLONE_SCRIPTS", scriptLink, scriptPath);

      for (VirtualDeploymentUnit virtualDeploymentUnit : virtualNetworkFunctionRecord.getVdu()) {
        for (VNFCInstance vnfcInstance : virtualDeploymentUnit.getVnfc_instance()) {
          executeActionOnEMS(
              vnfcInstance.getHostname(),
              jsonMessage.toString(),
              virtualNetworkFunctionRecord,
              vnfcInstance);
        }
      }
    } else if (scripts instanceof Set) {
      Iterable<Script> scriptSet = (Set<Script>) scripts;

      for (Script script : scriptSet) {
        log.debug("Sending script encoded base64 ");
        String base64String = Base64.encodeBase64String(script.getPayload());
        log.trace("The base64 string is: " + base64String);
        JsonObject jsonMessage =
            JsonUtils.getJsonObjectForScript(
                "SAVE_SCRIPTS", base64String, script.getName(), scriptPath);
        for (VirtualDeploymentUnit virtualDeploymentUnit : virtualNetworkFunctionRecord.getVdu()) {
          for (VNFCInstance vnfcInstance : virtualDeploymentUnit.getVnfc_instance()) {
            executeActionOnEMS(
                vnfcInstance.getHostname(),
                jsonMessage.toString(),
                virtualNetworkFunctionRecord,
                vnfcInstance);
          }
        }
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void saveScriptOnEms(
      VNFCInstance vnfcInstance,
      Object scripts,
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord)
      throws Exception {

    log.debug("Scripts are: " + scripts.getClass().getName());

    if (scripts instanceof String) {
      String scriptLink = (String) scripts;
      log.debug("Scripts are: " + scriptLink);
      JsonObject jsonMessage = JsonUtils.getJsonObject("CLONE_SCRIPTS", scriptLink, scriptPath);
      executeActionOnEMS(
          vnfcInstance.getHostname(),
          jsonMessage.toString(),
          virtualNetworkFunctionRecord,
          vnfcInstance);
    } else if (scripts instanceof Set) {
      Iterable<Script> scriptSet = (Iterable<Script>) scripts;
      for (Script script : scriptSet) {
        log.debug("Sending script encoded base64 ");
        String base64String = Base64.encodeBase64String(script.getPayload());
        log.trace("The base64 string is: " + base64String);
        JsonObject jsonMessage =
            JsonUtils.getJsonObjectForScript(
                "SAVE_SCRIPTS", base64String, script.getName(), scriptPath);
        executeActionOnEMS(
            vnfcInstance.getHostname(),
            jsonMessage.toString(),
            virtualNetworkFunctionRecord,
            vnfcInstance);
      }
    }
  }

  @Override
  public void saveVNFRecordDependency(
      VirtualNetworkFunctionRecord vnfr,
      VNFCInstance vnfcInstance,
      VNFRecordDependency vnfRecordDependency)
      throws Exception {

    String vnfRecordDependencyJson = parser.toJson(vnfRecordDependency);

    JsonObject jsonMessage =
        JsonUtils.getJsonObject(
            "SAVE_VNFR_DEPENDENCY", vnfRecordDependencyJson, parametersFilePath);

    executeActionOnEMS(vnfcInstance.getHostname(), jsonMessage.toString(), vnfr, vnfcInstance);
  }

  @Override
  public String executeActionOnEMS(
      String vduHostname,
      String command,
      VirtualNetworkFunctionRecord vnfr,
      VNFCInstance vnfcInstance)
      throws Exception {
    log.info("Waiting answer from EMS - " + vduHostname);

    String queueName = "vnfm." + vduHostname + ".actions";
    log.debug(String.format("Sending message %s to %s and waiting", command, queueName));
    String response = vnfmHelper.sendAndReceive(command, queueName);

    log.debug("Received from EMS (" + vduHostname + "): " + response);

    if (response == null) {
      throw new NullPointerException("Response from EMS is null");
    }

    JsonObject jsonObject = parser.fromJson(response, JsonObject.class);

    if ((jsonObject.get("status").getAsInt() == 0)) {
      try {
        log.debug("Output from EMS (" + vduHostname + ") is: " + jsonObject.get("output"));
      } catch (Exception e) {
        e.printStackTrace();
        throw new Exception(e);
      }
    } else {
      String err = jsonObject.get("err").getAsString();
      log.error(err);
      vnfcInstance.setState("error");
      logUtils.saveLogToFile(
          vnfr,
          parser.fromJson(command, JsonObject.class).get("payload").getAsString(),
          vnfcInstance,
          response,
          true);
      throw new Exception(err);
    }
    return response;
  }

  @Override
  public void removeRegistrationUnit(EmsRegistrationUnit unit) {
    this.log.debug("EMSRegister removing: " + unit);
    registrationUnits =
        registrationUnits
            .stream()
            .filter(h -> !h.getValue().equals(unit.getValue()))
            .collect(Collectors.toSet());
  }

  @Override
  public String getEmsHeartbeat() {
    return emsConfiguration.getHeartbeat();
  }

  @Override
  public String getEmsAutodelete() {
    return Boolean.toString(emsConfiguration.isAutodelete());
  }

  @Override
  public String getEmsVersion() {
    return emsConfiguration.getVersion();
  }

  // It is supported for ems version >= 1.1.0
  @Override
  public boolean isSaveVNFRecordDependencySupported() {
    String[] emsVersionSplitted = getEmsVersion().split("\\.");
    return emsVersionSplitted.length >= 2
        && (emsVersionSplitted[0].compareTo("1") >= 0)
        && (emsVersionSplitted[1].compareTo("1") >= 0);
  }
}
