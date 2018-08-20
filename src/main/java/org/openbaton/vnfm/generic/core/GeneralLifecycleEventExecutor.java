/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
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
