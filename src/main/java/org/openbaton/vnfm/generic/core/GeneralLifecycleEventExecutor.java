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
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.common.vnfm_sdk.exception.VnfmSdkException;
import org.openbaton.vnfm.generic.utils.EnvMapUtils;
import org.openbaton.vnfm.generic.utils.JsonUtils;

public class GeneralLifecycleEventExecutor extends LifecycleEventExecutor {

  public GeneralLifecycleEventExecutor(
      LifecycleEvent lifecycleEvent, VirtualNetworkFunctionRecord vnfr) {
    super(lifecycleEvent, vnfr);
  }

  @Override
  protected Collection<String> execute() throws VnfmSdkException {
    Collection<String> result = new ArrayList<>();
    for (String script : scripts) {
      log.info("Sending script: " + script + " to VirtualNetworkFunctionRecord: " + vnfr.getName());
      for (VNFCInstance vnfcInstance : getVnfcInstances()) {
        Map<String, String> tempEnv = createEnvMapFrom(vnfcInstance);
        env.putAll(tempEnv);
        log.info("Environment Variables are: " + env);
        String action = JsonUtils.getJsonObject("EXECUTE", script, env).toString();
        String output = executeActionOnEMS(action, vnfcInstance, script);
        result.add(output);
        env = EnvMapUtils.clearEnvFromTempValues(env, tempEnv);
      }
    }
    return result;
  }
}
