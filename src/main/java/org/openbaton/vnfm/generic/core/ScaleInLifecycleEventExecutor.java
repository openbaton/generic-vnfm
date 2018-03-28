/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.openbaton.vnfm.generic.core;

import java.util.Map;
import org.openbaton.catalogue.mano.common.Ip;
import org.openbaton.catalogue.mano.common.LifecycleEvent;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.vnfm.generic.utils.EnvMapUtils;

public class ScaleInLifecycleEventExecutor extends GeneralLifecycleEventExecutor {
  private VNFCInstance removedVnfcInstance;

  public ScaleInLifecycleEventExecutor(
      LifecycleEvent lifecycleEvent,
      VirtualNetworkFunctionRecord vnfr,
      VNFCInstance removedVnfcInstance) {
    super(lifecycleEvent, vnfr);
    setRemovedVnfcInstance(removedVnfcInstance);
  }

  @Override
  protected Map<String, String> createEnvMapFrom(VNFCInstance vnfcInstance) {
    Map<String, String> envMap =
        EnvMapUtils.createForLifeCycleEventExecutionOnVNFCInstance(vnfcInstance);
    envMap = putInfoFrom(envMap, removedVnfcInstance);
    return envMap;
  }

  private Map<String, String> putInfoFrom(
      Map<String, String> envMap, VNFCInstance removedVnfcInstance) {
    for (Ip ip : removedVnfcInstance.getIps()) {
      log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
      envMap.put("removing_" + ip.getNetName(), ip.getIp());
    }
    log.debug("adding floatingIp: " + removedVnfcInstance.getFloatingIps());
    for (Ip fip : removedVnfcInstance.getFloatingIps()) {
      envMap.put("removing_" + fip.getNetName() + "_floatingIp", fip.getIp());
    }
    envMap.put("removing_" + "hostname", removedVnfcInstance.getHostname());
    return envMap;
  }

  private VNFCInstance getRemovedVnfcInstance() {
    return removedVnfcInstance;
  }

  private void setRemovedVnfcInstance(VNFCInstance removedVnfcInstance) {
    this.removedVnfcInstance = removedVnfcInstance;
  }
}
