/*
 * Copyright (c) 2015-2018 Open Baton (http://openbaton.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.vnfm.generic.core;

import java.util.*;
import org.openbaton.catalogue.mano.common.LifecycleEvent;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.vnfm.generic.utils.EnvMapUtils;
import org.openbaton.vnfm.generic.utils.JsonUtils;

public class ConfigureLifecycleEventExecutor extends LifecycleEventExecutor {

  private VNFRecordDependency vnfRecordDependency;

  public ConfigureLifecycleEventExecutor(
      LifecycleEvent lifecycleEvent,
      VirtualNetworkFunctionRecord vnfr,
      VNFRecordDependency vnfRecordDependency) {
    super(lifecycleEvent, vnfr);
    this.vnfRecordDependency = vnfRecordDependency;
  }

  @Override
  protected Collection<String> execute() throws Exception {
    Collection<String> result = new ArrayList<>();
    boolean dependencyAlreadySaved = false;
    boolean dependencySavedForVNFCInstance = false;
    for (String script : scripts) {
      String foreignDependencyType = getForeingDependencyType(script);
      for (VNFCInstance vnfcInstance : getVnfcInstances()) {
        env = createEnvMapFrom(env, vnfcInstance);

        // check if the script starts with type
        if (foreignDependencyType != null
            && vnfRecordDependency.getVnfcParameters().get(foreignDependencyType) != null) {
          // send execute for each dependency
          for (String vnfcId :
              vnfRecordDependency
                  .getVnfcParameters()
                  .get(foreignDependencyType)
                  .getParameters()
                  .keySet()) {

            Map<String, String> tempEnv = new HashMap<>();

            //Adding foreign parameters such as ip
            log.debug("Fetching parameter from dependency of type: " + foreignDependencyType);
            Map<String, String> parameters =
                vnfRecordDependency.getParameters().get(foreignDependencyType).getParameters();

            for (Map.Entry<String, String> param : parameters.entrySet()) {
              log.debug(
                  "adding param: "
                      + foreignDependencyType
                      + "_"
                      + param.getKey()
                      + " = "
                      + param.getValue());
              tempEnv.put(foreignDependencyType + "_" + param.getKey(), param.getValue());
            }

            Map<String, String> parametersVNFC =
                vnfRecordDependency
                    .getVnfcParameters()
                    .get(foreignDependencyType)
                    .getParameters()
                    .get(vnfcId)
                    .getParameters();
            for (Map.Entry<String, String> param : parametersVNFC.entrySet()) {
              log.debug(
                  "adding param: "
                      + foreignDependencyType
                      + "_"
                      + param.getKey()
                      + " = "
                      + param.getValue());
              tempEnv.put(foreignDependencyType + "_" + param.getKey(), param.getValue());
            }

            tempEnv = EnvMapUtils.modifyUnsafeEnvVarNames(tempEnv);
            env.putAll(tempEnv);
            log.info("Environment Variables are: " + env);
            //TODO remove "script" from executeActionOnEMS arguments
            String action = JsonUtils.getJsonObject("EXECUTE", script, env).toString();
            String output = executeActionOnEMS(action, vnfcInstance, script);
            result.add(output);
            env = EnvMapUtils.clearEnvFromTempValues(env, tempEnv);
          }
        }
        // the script does not begin with "<type>_" so it will be executed only once
        // like a script in the INSTANTIATE lifecycle event
        else {
          if (!dependencyAlreadySaved && ems.isSaveVNFRecordDependencySupported()) {

            ems.saveVNFRecordDependency(vnfr, vnfcInstance, vnfRecordDependency);
            dependencySavedForVNFCInstance = true;
          }
          String action = JsonUtils.getJsonObject("EXECUTE", script, env).toString();
          String output = executeActionOnEMS(action, vnfcInstance, script);
          result.add(output);
        }
        env = EnvMapUtils.clearOwnIpAndFloatingIpInEnv(env, vnfcInstance);
        env = EnvMapUtils.clearVNFCInstanceHostnameInMap(env);
      }
      // Prevent the saveVNFRecordDependency to be called
      // multiple times for multiple scripts
      dependencyAlreadySaved = dependencySavedForVNFCInstance;
    }
    return result;
  }

  private String getForeingDependencyType(String script) {
    String foreignDependencyType = null;
    boolean scriptContainsUnderscore = script.contains("_");
    if (scriptContainsUnderscore) {
      foreignDependencyType = script.substring(0, script.indexOf('_'));
    }
    return foreignDependencyType;
  }
}
