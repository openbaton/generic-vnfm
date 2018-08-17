/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.openbaton.vnfm.generic.core;

import java.util.Map;
import org.openbaton.catalogue.mano.common.LifecycleEvent;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.vnfm.generic.utils.EnvMapUtils;
import org.springframework.util.Assert;

public class HealLifecycleEventExecutor extends GeneralLifecycleEventExecutor {

  private String cause;

  public HealLifecycleEventExecutor(
      LifecycleEvent lifecycleEvent, VirtualNetworkFunctionRecord vnfr, String cause) {
    super(lifecycleEvent, vnfr);
    setCause(cause);
  }

  @Override
  protected Map<String, String> createEnvMapFrom(VNFCInstance vnfcInstance) {
    Map<String, String> envMap =
        EnvMapUtils.createForLifeCycleEventExecutionOnVNFCInstance(vnfcInstance);
    envMap.put("cause", getCause());
    return envMap;
  }

  private String getCause() {
    return cause;
  }

  public void setCause(String cause) {
    Assert.notNull(cause, "Cause cannot be null");
    this.cause = cause;
  }
}
