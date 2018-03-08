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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.openbaton.catalogue.mano.common.Event;
import org.openbaton.catalogue.mano.common.Ip;
import org.openbaton.catalogue.mano.common.LifecycleEvent;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.ConfigurationParameter;
import org.openbaton.catalogue.nfvo.VNFCDependencyParameters;
import org.openbaton.common.vnfm_sdk.utils.VnfmUtils;
import org.openbaton.vnfm.generic.utils.JsonUtils;
import org.openbaton.vnfm.generic.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** Created by lto on 15/09/15. */
@Service
@Scope
public class LifeCycleManagement {

  private Logger log = LoggerFactory.getLogger(this.getClass());
  private static Gson parser = new GsonBuilder().setPrettyPrinting().create();

  @Autowired private ElementManagementSystem ems;
  @Autowired private LogUtils logUtils;

  public Iterable<String> executeScriptsForEvent(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Event event)
      throws Exception { //TODO make it parallel
    Map<String, String> env = getMap(virtualNetworkFunctionRecord);
    Collection<String> res = new ArrayList<>();
    LifecycleEvent le =
        VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), event);

    if (le != null) {
      log.trace(
          "The number of scripts for "
              + virtualNetworkFunctionRecord.getName()
              + " are: "
              + le.getLifecycle_events());
      for (String script : le.getLifecycle_events()) {
        log.info(
            "Sending script: "
                + script
                + " to VirtualNetworkFunctionRecord: "
                + virtualNetworkFunctionRecord.getName());
        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
          for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {

            Map<String, String> tempEnv = new HashMap<>();
            for (Ip ip : vnfcInstance.getIps()) {
              log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
              tempEnv.put(ip.getNetName(), ip.getIp());
            }
            log.debug("adding floatingIp: " + vnfcInstance.getFloatingIps());
            for (Ip fip : vnfcInstance.getFloatingIps()) {
              tempEnv.put(fip.getNetName() + "_floatingIp", fip.getIp());
            }

            tempEnv.put("hostname", vnfcInstance.getHostname());
            tempEnv = modifyUnsafeEnvVarNames(tempEnv);
            env.putAll(tempEnv);
            log.info("Environment Variables are: " + env);

            String command = JsonUtils.getJsonObject("EXECUTE", script, env).toString();
            String output =
                ems.executeActionOnEMS(
                    vnfcInstance.getHostname(),
                    command,
                    virtualNetworkFunctionRecord,
                    vnfcInstance);
            res.add(output);
            log.debug("Saving log to file...");
            logUtils.saveLogToFile(virtualNetworkFunctionRecord, script, vnfcInstance, output);
            for (String key : tempEnv.keySet()) {
              env.remove(key);
            }
          }
        }
      }
    }
    return res;
  }

  private Map<String, String> setOwnIpsInEnv(Map<String, String> env, VNFCInstance vnfcInstance) {
    //Adding own ips
    for (Ip ip : vnfcInstance.getIps()) {
      log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
      env.put(ip.getNetName(), ip.getIp());
    }

    //Adding own floating ip
    for (Ip fip : vnfcInstance.getFloatingIps()) {
      log.debug("adding floatingIp: " + fip.getNetName() + " = " + fip.getIp());
      env.put(fip.getNetName() + "_floatingIp", fip.getIp());
    }
    return env;
  }

  private Map<String, String> clearOwnIpsInEnv(Map<String, String> env, VNFCInstance vnfcInstance) {
    //Clearing own ips
    for (Ip ip : vnfcInstance.getIps()) {
      env.remove(ip.getNetName());
    }
    //Clearing own floating ip
    for (Ip fip : vnfcInstance.getFloatingIps()) {
      env.remove(fip.getNetName() + "_floatingIp");
    }
    return env;
  }

  public Iterable<String> executeScriptsForEvent(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      Event event,
      VNFRecordDependency dependency)
      throws Exception {
    Map<String, String> env = getMap(virtualNetworkFunctionRecord);
    LifecycleEvent le =
        VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), event);
    List<String> res = new ArrayList<>();
    log.debug("vnfr dependency: " + parser.toJson(dependency));

    if (le != null) {
      for (String script : le.getLifecycle_events()) {
        boolean dependencySaved = false;
        String type = null;
        if (script.contains("_")) {
          type = script.substring(0, script.indexOf('_'));
          log.info(
              "Sending command: "
                  + script
                  + " to adding relation with type: "
                  + type
                  + " from VirtualNetworkFunctionRecord "
                  + virtualNetworkFunctionRecord.getName());
        }

        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
          for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {

            // add own ips and floating ip to env
            env = setOwnIpsInEnv(env, vnfcInstance);
            // add hostname to env
            env.put("hostname", vnfcInstance.getHostname());

            // check if the script starts with type
            if (dependency.getVnfcParameters().get(type) != null) {
              // send execute for each dependency
              for (String vnfcId :
                  dependency.getVnfcParameters().get(type).getParameters().keySet()) {

                Map<String, String> tempEnv = new HashMap<>();

                if (script.contains("_")) {
                  //Adding foreign parameters such as ip
                  log.debug("Fetching parameter from dependency of type: " + type);
                  Map<String, String> parameters =
                      dependency.getParameters().get(type).getParameters();

                  for (Map.Entry<String, String> param : parameters.entrySet()) {
                    log.debug(
                        "adding param: " + type + "_" + param.getKey() + " = " + param.getValue());
                    tempEnv.put(type + "_" + param.getKey(), param.getValue());
                  }

                  Map<String, String> parametersVNFC =
                      dependency
                          .getVnfcParameters()
                          .get(type)
                          .getParameters()
                          .get(vnfcId)
                          .getParameters();
                  for (Map.Entry<String, String> param : parametersVNFC.entrySet()) {
                    log.debug(
                        "adding param: " + type + "_" + param.getKey() + " = " + param.getValue());
                    tempEnv.put(type + "_" + param.getKey(), param.getValue());
                  }
                }

                tempEnv = modifyUnsafeEnvVarNames(tempEnv);
                env.putAll(tempEnv);
                log.info("Environment Variables are: " + env);

                String command = JsonUtils.getJsonObject("EXECUTE", script, env).toString();
                String output =
                    ems.executeActionOnEMS(
                        vnfcInstance.getHostname(),
                        command,
                        virtualNetworkFunctionRecord,
                        vnfcInstance);
                res.add(output);

                logUtils.saveLogToFile(virtualNetworkFunctionRecord, script, vnfcInstance, output);
                for (String key : tempEnv.keySet()) {
                  env.remove(key);
                }
              }
            }
            // the script does not begin with "<type>_" so it will be executed only once
            // like a script in the INSTANTIATE lifecycle event
            else {
              // save dependency in the ems
              if (!dependencySaved) {
                ems.saveVNFRecordDependencyOnEms(
                    virtualNetworkFunctionRecord, vnfcInstance, dependency);
                dependencySaved = true;
              }

              String command = JsonUtils.getJsonObject("EXECUTE", script, env).toString();
              String output =
                  ems.executeActionOnEMS(
                      vnfcInstance.getHostname(),
                      command,
                      virtualNetworkFunctionRecord,
                      vnfcInstance);
              res.add(output);
              logUtils.saveLogToFile(virtualNetworkFunctionRecord, script, vnfcInstance, output);
            }

            // clean own ips and hostname in env
            env = clearOwnIpsInEnv(env, vnfcInstance);
            env.remove("hostname");
          }
        }
      }
    }
    return res;
  }

  public Iterable<String> executeScriptsForEvent(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      VNFCInstance vnfcInstance,
      Event event,
      VNFRecordDependency dependency)
      throws Exception {
    Map<String, String> env = getMap(virtualNetworkFunctionRecord);
    List<String> res = new ArrayList<>();
    LifecycleEvent le =
        VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), event);
    log.trace(
        "The number of scripts for "
            + virtualNetworkFunctionRecord.getName()
            + " are: "
            + le.getLifecycle_events());
    log.debug("DEPENDENCY IS: " + dependency);
    if (le != null) {
      for (String script : le.getLifecycle_events()) {
        int indexOf = script.indexOf('_');
        VNFCDependencyParameters vnfcDependencyParameters = null;
        String type = null;
        if (indexOf != -1) {
          type = script.substring(0, indexOf);
          vnfcDependencyParameters = dependency.getVnfcParameters().get(type);
        }
        if (vnfcDependencyParameters != null) {
          log.debug(
              "There are "
                  + vnfcDependencyParameters.getParameters().size()
                  + " VNFCInstanceForeign");
          for (String vnfcForeignId : vnfcDependencyParameters.getParameters().keySet()) {
            log.info("Running script: " + script + " for VNFCInstance foreign id " + vnfcForeignId);

            log.info(
                "Sending command: "
                    + script
                    + " to adding relation with type: "
                    + type
                    + " from VirtualNetworkFunctionRecord "
                    + virtualNetworkFunctionRecord.getName());

            Map<String, String> tempEnv = new HashMap<>();

            //Adding own ips
            for (Ip ip : vnfcInstance.getIps()) {
              log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
              tempEnv.put(ip.getNetName(), ip.getIp());
            }

            //Adding own floating ip
            log.debug("adding floatingIp: " + vnfcInstance.getFloatingIps());
            for (Ip fip : vnfcInstance.getFloatingIps()) {
              tempEnv.put(fip.getNetName() + "_floatingIp", fip.getIp());
            }
            //Adding foreign parameters such as ip
            if (script.contains("_")) {
              //Adding foreign parameters such as ip
              Map<String, String> parameters = dependency.getParameters().get(type).getParameters();
              for (Map.Entry<String, String> param : parameters.entrySet()) {
                tempEnv.put(type + "_" + param.getKey(), param.getValue());
              }

              Map<String, String> parametersVNFC =
                  vnfcDependencyParameters.getParameters().get(vnfcForeignId).getParameters();
              for (Map.Entry<String, String> param : parametersVNFC.entrySet()) {
                tempEnv.put(type + "_" + param.getKey(), param.getValue());
              }
            }

            tempEnv.put("hostname", vnfcInstance.getHostname());
            tempEnv = modifyUnsafeEnvVarNames(tempEnv);
            env.putAll(tempEnv);
            log.info("The Environment Variables for script " + script + " are: " + env);

            String command = JsonUtils.getJsonObject("EXECUTE", script, env).toString();
            String output =
                ems.executeActionOnEMS(
                    vnfcInstance.getHostname(),
                    command,
                    virtualNetworkFunctionRecord,
                    vnfcInstance);
            res.add(output);

            logUtils.saveLogToFile(virtualNetworkFunctionRecord, script, vnfcInstance, output);
            for (String key : tempEnv.keySet()) {
              env.remove(key);
            }
          }
        }
      }
    }
    return res;
  }

  public Iterable<String> executeScriptsForEvent(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      VNFCInstance vnfcInstance,
      Event event,
      String cause)
      throws Exception {
    Map<String, String> env = getMap(virtualNetworkFunctionRecord);
    List<String> res = new LinkedList<>();
    LifecycleEvent le =
        VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), event);

    if (le != null) {
      log.trace(
          "The number of scripts for "
              + virtualNetworkFunctionRecord.getName()
              + " are: "
              + le.getLifecycle_events());
      for (String script : le.getLifecycle_events()) {
        log.info(
            "Sending script: "
                + script
                + " to VirtualNetworkFunctionRecord: "
                + virtualNetworkFunctionRecord.getName());
        Map<String, String> tempEnv = new HashMap<>();
        for (Ip ip : vnfcInstance.getIps()) {
          log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
          tempEnv.put(ip.getNetName(), ip.getIp());
        }
        log.debug("adding floatingIp: " + vnfcInstance.getFloatingIps());
        for (Ip fip : vnfcInstance.getFloatingIps()) {
          tempEnv.put(fip.getNetName() + "_floatingIp", fip.getIp());
        }

        tempEnv.put("hostname", vnfcInstance.getHostname());
        //Add cause to the environment variables
        tempEnv.put("cause", cause);

        tempEnv = modifyUnsafeEnvVarNames(tempEnv);
        env.putAll(tempEnv);
        log.info("The Environment Variables for script " + script + " are: " + env);

        String command = JsonUtils.getJsonObject("EXECUTE", script, env).toString();
        String output =
            ems.executeActionOnEMS(
                vnfcInstance.getHostname(), command, virtualNetworkFunctionRecord, vnfcInstance);
        res.add(output);
        logUtils.saveLogToFile(virtualNetworkFunctionRecord, script, vnfcInstance, output);
        for (String key : tempEnv.keySet()) {
          env.remove(key);
        }
      }
    }
    return res;
  }

  private Map<String, String> modifyUnsafeEnvVarNames(Map<String, String> env) {

    Map<String, String> result = new HashMap<>();

    for (Map.Entry<String, String> entry : env.entrySet()) {
      result.put(entry.getKey().replaceAll("[^A-Za-z0-9_]", "_"), entry.getValue());
    }

    return result;
  }

  public Iterable<String> executeScriptsForEvent(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      VNFCInstance vnfcInstance,
      Event event)
      throws Exception {
    Map<String, String> env = getMap(virtualNetworkFunctionRecord);
    List<String> res = new ArrayList<>();
    LifecycleEvent le =
        VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), event);

    if (le != null) {
      log.trace(
          "The number of scripts for "
              + virtualNetworkFunctionRecord.getName()
              + " are: "
              + le.getLifecycle_events());
      for (String script : le.getLifecycle_events()) {
        log.info(
            "Sending script: "
                + script
                + " to VirtualNetworkFunctionRecord: "
                + virtualNetworkFunctionRecord.getName());
        Map<String, String> tempEnv = new HashMap<>();
        for (Ip ip : vnfcInstance.getIps()) {
          log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
          tempEnv.put(ip.getNetName(), ip.getIp());
        }
        log.debug("adding floatingIp: " + vnfcInstance.getFloatingIps());
        for (Ip fip : vnfcInstance.getFloatingIps()) {
          tempEnv.put(fip.getNetName() + "_floatingIp", fip.getIp());
        }

        tempEnv.put("hostname", vnfcInstance.getHostname());

        tempEnv = modifyUnsafeEnvVarNames(tempEnv);
        env.putAll(tempEnv);
        log.info("The Environment Variables for script " + script + " are: " + env);

        String command = JsonUtils.getJsonObject("EXECUTE", script, env).toString();
        if (event.ordinal() == Event.SCALE_IN.ordinal()) {
          for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
            for (VNFCInstance vnfcInstance1 : vdu.getVnfc_instance()) {

              String output =
                  ems.executeActionOnEMS(
                      vnfcInstance1.getHostname(),
                      command,
                      virtualNetworkFunctionRecord,
                      vnfcInstance);
              res.add(output);
              logUtils.saveLogToFile(virtualNetworkFunctionRecord, script, vnfcInstance1, output);
            }
          }
        } else {
          res.add(
              ems.executeActionOnEMS(
                  vnfcInstance.getHostname(), command, virtualNetworkFunctionRecord, vnfcInstance));
        }

        for (String key : tempEnv.keySet()) {
          env.remove(key);
        }
      }
    }
    return res;
  }

  public Iterable<? extends String> executeScriptsForEventOnVnfr(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      VNFCInstance vnfcInstanceRemote,
      Event event)
      throws Exception {
    Map<String, String> env = getMap(virtualNetworkFunctionRecord);
    Collection<String> res = new ArrayList<>();
    LifecycleEvent le =
        VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), event);
    if (le != null) {
      log.trace(
          "The number of scripts for "
              + virtualNetworkFunctionRecord.getName()
              + " are: "
              + le.getLifecycle_events());
      for (VirtualDeploymentUnit virtualDeploymentUnit : virtualNetworkFunctionRecord.getVdu()) {
        for (VNFCInstance vnfcInstanceLocal : virtualDeploymentUnit.getVnfc_instance()) {
          for (String script : le.getLifecycle_events()) {
            log.info(
                "Sending script: "
                    + script
                    + " to VirtualNetworkFunctionRecord: "
                    + virtualNetworkFunctionRecord.getName()
                    + " on VNFCInstance: "
                    + vnfcInstanceLocal.getId());
            Map<String, String> tempEnv = new HashMap<>();
            for (Ip ip : vnfcInstanceLocal.getIps()) {
              log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
              tempEnv.put(ip.getNetName(), ip.getIp());
            }
            log.debug("adding floatingIp: " + vnfcInstanceLocal.getFloatingIps());
            for (Ip fip : vnfcInstanceLocal.getFloatingIps()) {
              tempEnv.put(fip.getNetName() + "_floatingIp", fip.getIp());
            }

            tempEnv.put("hostname", vnfcInstanceLocal.getHostname());

            if (vnfcInstanceRemote != null) {
              //TODO what should i put here?
              for (Ip ip : vnfcInstanceRemote.getIps()) {
                log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
                tempEnv.put("removing_" + ip.getNetName(), ip.getIp());
              }
              log.debug("adding floatingIp: " + vnfcInstanceRemote.getFloatingIps());
              for (Ip fip : vnfcInstanceRemote.getFloatingIps()) {
                tempEnv.put("removing_" + fip.getNetName() + "_floatingIp", fip.getIp());
              }

              tempEnv.put("removing_" + "hostname", vnfcInstanceRemote.getHostname());
            }

            tempEnv = modifyUnsafeEnvVarNames(tempEnv);
            env.putAll(tempEnv);
            log.info("The Environment Variables for script " + script + " are: " + env);

            String command = JsonUtils.getJsonObject("EXECUTE", script, env).toString();
            String output =
                ems.executeActionOnEMS(
                    vnfcInstanceLocal.getHostname(),
                    command,
                    virtualNetworkFunctionRecord,
                    vnfcInstanceLocal);
            res.add(output);

            logUtils.saveLogToFile(virtualNetworkFunctionRecord, script, vnfcInstanceLocal, output);
            for (String key : tempEnv.keySet()) {
              env.remove(key);
            }
          }
        }
      }
    }
    return res;
  }

  private Map<String, String> getMap(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
    Map<String, String> res = new HashMap<>();
    for (ConfigurationParameter configurationParameter :
        virtualNetworkFunctionRecord.getProvides().getConfigurationParameters()) {
      res.put(configurationParameter.getConfKey(), configurationParameter.getValue());
    }
    for (ConfigurationParameter configurationParameter :
        virtualNetworkFunctionRecord.getConfigurations().getConfigurationParameters()) {
      res.put(configurationParameter.getConfKey(), configurationParameter.getValue());
    }
    res = modifyUnsafeEnvVarNames(res);
    return res;
  }
}
