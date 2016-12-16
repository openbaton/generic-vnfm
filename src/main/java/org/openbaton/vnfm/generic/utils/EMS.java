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

package org.openbaton.vnfm.generic.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Base64;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.Script;
import org.openbaton.common.vnfm_sdk.VnfmHelper;
import org.openbaton.common.vnfm_sdk.exception.BadFormatException;
import org.openbaton.common.vnfm_sdk.exception.VnfmSdkException;
import org.openbaton.vnfm.generic.interfaces.EmsInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lto on 15/09/15.
 */
@Service
@Scope
public class EMS implements EmsInterface {

  private static Gson parser = new GsonBuilder().setPrettyPrinting().create();

  private Logger log = LoggerFactory.getLogger(getClass());

  @Value("${vnfm.ems.start.timeout:500}")
  private int waitForEms;

  @Value("${vnfm.ems.queue.heartbeat}")
  protected String emsHeartbeat;

  @Value("${vnfm.ems.queue.autodelete}")
  protected String emsAutodelete;

  @Value("${vnfm.ems.version}")
  protected String emsVersion;

  //TODO consider using DB in case of failure etc...
  private Set<String> expectedHostnames;

  private String scriptPath;

  private VnfmHelper vnfmHelper;

  public void init(String scriptPath, VnfmHelper vnfmHelper) {
    this.scriptPath = scriptPath;
    this.expectedHostnames = new HashSet<>();
    this.vnfmHelper = vnfmHelper;
  }

  public Set<String> getExpectedHostnames() {
    return this.expectedHostnames;
  }

  public void register(String hostname) {
    this.log.debug("EMSRegister adding: " + hostname);
    this.expectedHostnames.add(hostname);
  }

  public void unregister(String hostname) {
    this.log.debug("EMSRegister removing: " + hostname);
    if (this.expectedHostnames.contains(hostname)) this.expectedHostnames.remove(hostname);
  }

  @Override
  public void unregisterFromMsg(String json) {
    this.log.debug("EMSRegister received: " + json);
    JsonObject object = this.parser.fromJson(json, JsonObject.class);
    String hostname = object.get("hostname").getAsString();
    this.log.debug("EMSRegister removing: " + hostname);
    if (this.expectedHostnames.contains(hostname)) this.expectedHostnames.remove(hostname);
  }

  @Override
  public void checkEms(String hostname) throws BadFormatException {
    log.debug("Starting wait of EMS for: " + hostname);
    this.register(hostname);
    String extractedId = "";
    Pattern pattern = Pattern.compile("-(\\d+)$");
    Matcher matcher = pattern.matcher(hostname.trim());
    if (matcher.find()) {
      extractedId = (matcher.group(1));
    } else {
      throw new BadFormatException(
          "Hostname does not fit the expected format. Must fit: '.*-[1-9]+$'");
    }
    log.trace("Extracted host ID: " + extractedId);
    int i = 0;
    while (true) {
      log.debug("Number of expected EMS hostnames: " + this.getExpectedHostnames().size());
      log.debug("Waiting for " + hostname + " EMS to be started... (" + i * 5 + " secs)");
      i++;
      try {
        checkEmsStarted(extractedId);
        break;
      } catch (RuntimeException e) {
        if (i == this.waitForEms / 5) {
          throw e;
        }
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      }
    }
  }

  @Override
  public void checkEmsStarted(String hostId) throws BadFormatException {
    boolean registered = true;
    log.debug("Expected hostnames: " + this.getExpectedHostnames());
    for (String expectedHostname : this.getExpectedHostnames()) {
      if (expectedHostname.endsWith(hostId)) {
        registered = false;
        break;
      }
    }
    if (registered == false) {
      throw new RuntimeException("No EMS yet for host with extracted host ID: " + hostId);
    }
  }

  @Override
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
      Iterable<Script> scriptSet = (Set<Script>) scripts;
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
  public String executeActionOnEMS(
      String vduHostname,
      String command,
      VirtualNetworkFunctionRecord vnfr,
      VNFCInstance vnfcInstance)
      throws Exception {
    log.debug("Sending message and waiting: " + command + " to " + vduHostname);
    log.info("Waiting answer from EMS - " + vduHostname);

    String response =
        vnfmHelper.sendAndReceive(
            command, "vnfm." + vduHostname.toLowerCase().replace("_", "-") + ".actions");

    log.debug("Received from EMS (" + vduHostname + "): " + response);

    if (response == null) {
      throw new NullPointerException("Response from EMS is null");
    }

    JsonObject jsonObject = this.parser.fromJson(response, JsonObject.class);

    if (jsonObject.get("status").getAsInt() == 0) {
      try {
        log.debug("Output from EMS (" + vduHostname + ") is: " + jsonObject.get("output"));
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }
    } else {
      String err = jsonObject.get("err").getAsString();
      log.error(err);
      vnfcInstance.setState("error");
      LogUtils.saveLogToFile(
          vnfr,
          parser.fromJson(command, JsonObject.class).get("payload").getAsString(),
          vnfcInstance,
          response,
          true);
      throw new VnfmSdkException("EMS (" + vduHostname + ") had the following error: " + err);
    }
    return response;
  }

  @Override
  public String getEmsHeartbeat() {
    return emsHeartbeat;
  }

  @Override
  public String getEmsAutodelete() {
    return emsAutodelete;
  }

  @Override
  public String getEmsVersion() {
    return emsVersion;
  }
}
