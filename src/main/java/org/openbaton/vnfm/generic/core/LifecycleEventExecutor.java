/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.openbaton.vnfm.generic.core;

import java.util.*;
import org.openbaton.catalogue.mano.common.Event;
import org.openbaton.catalogue.mano.common.LifecycleEvent;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.common.vnfm_sdk.exception.VnfmSdkException;
import org.openbaton.vnfm.generic.interfaces.EmsInterface;
import org.openbaton.vnfm.generic.model.VNFRErrorStatus;
import org.openbaton.vnfm.generic.repository.VNFRErrorRepository;
import org.openbaton.vnfm.generic.utils.EnvMapUtils;
import org.openbaton.vnfm.generic.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public abstract class LifecycleEventExecutor {
  protected VirtualNetworkFunctionRecord vnfr;
  protected Event event;
  protected List<String> scripts;
  protected Map<String, String> env;
  protected Logger log = LoggerFactory.getLogger(this.getClass());
  protected EmsInterface ems;
  private LogUtils logUtils;
  private Set<VNFCInstance> vnfcInstances;
  private VNFRErrorRepository vnfrErrorRepository;

  public LifecycleEventExecutor(LifecycleEvent lifecycleEvent, VirtualNetworkFunctionRecord vnfr) {
    Assert.notNull(lifecycleEvent, "LifecycleEvent is null");
    setEvent(lifecycleEvent.getEvent());
    setScripts(lifecycleEvent.getLifecycle_events());
    setVnfr(vnfr);
    setEnv(EnvMapUtils.createConfigurationParametersAndProvidesMapFromVNFR(vnfr));
  }

  private void setEvent(Event event) {
    this.event = event;
  }

  public void setScripts(List<String> scripts) {
    Assert.notNull(scripts, "List of scripts is null");
    this.scripts = scripts;
  }

  private void setVnfr(VirtualNetworkFunctionRecord vnfr) {
    this.vnfr = vnfr;
  }

  private void setEnv(Map<String, String> env) {
    this.env = env;
  }

  public Collection<String> executeOn(VNFCInstance vnfcInstance) throws Exception {
    this.vnfcInstances = new HashSet<>();
    this.vnfcInstances.add(vnfcInstance);
    return execute();
  }

  protected abstract Collection<String> execute() throws Exception;

  protected String executeActionOnEMS(String action, VNFCInstance vnfcInstance, String script)
      throws VnfmSdkException {
    String output = "";
    try {
      output = sendActionToEMSAndSaveLogToFile(action, vnfcInstance, script);
    } catch (Exception e) {
      handleScriptException(e, vnfcInstance, script);
    }
    return output;
  }

  private String sendActionToEMSAndSaveLogToFile(
      String action, VNFCInstance vnfcInstance, String script) throws Exception {
    String output = sendActionToEMS(action, vnfcInstance);
    log.debug("Saving log to file...");
    logUtils.saveLogToFile(vnfr, script, vnfcInstance, output);
    return output;
  }

  private String sendActionToEMS(String action, VNFCInstance vnfcInstance) throws Exception {
    return ems.executeActionOnEMS(vnfcInstance.getHostname(), action, vnfr, vnfcInstance);
  }

  protected Set<VNFCInstance> getVnfcInstances() {
    if (vnfcInstances == null) {
      vnfcInstances = new HashSet<>();
      for (VirtualDeploymentUnit vdu : vnfr.getVdu()) vnfcInstances.addAll(vdu.getVnfc_instance());
    }
    return vnfcInstances;
  }

  protected Map<String, String> createEnvMapFrom(VNFCInstance vnfcInstance) {
    return EnvMapUtils.createForLifeCycleEventExecutionOnVNFCInstance(vnfcInstance);
  }

  private void handleScriptException(Exception e, VNFCInstance vnfcInstance, String script)
      throws VnfmSdkException {
    log.debug("Exception for vnfci: " + vnfcInstance.getId());
    Integer indexOfScript = scripts.indexOf(script);
    //to prevent creation of duplicate error record
    if (vnfrErrorRepository.findByVnfrIdAndEventAndScriptIndex(vnfr.getId(), event, indexOfScript)
        == null) createVnfrErrorRecord(indexOfScript);
    throw new VnfmSdkException(
        "EMS (" + vnfcInstance.getHostname() + ") had the following error:" + e);
  }

  private void createVnfrErrorRecord(Integer scriptIndex) {
    VNFRErrorStatus vnfrErrorStatus = new VNFRErrorStatus(vnfr.getId(), event, scriptIndex);
    vnfrErrorRepository.save(vnfrErrorStatus);
  }

  public void setEms(EmsInterface ems) {
    this.ems = ems;
  }

  public void setLogUtils(LogUtils logUtils) {
    this.logUtils = logUtils;
  }

  public void setVnfrErrorRepository(VNFRErrorRepository vnfrErrorRepository) {
    this.vnfrErrorRepository = vnfrErrorRepository;
  }
}
