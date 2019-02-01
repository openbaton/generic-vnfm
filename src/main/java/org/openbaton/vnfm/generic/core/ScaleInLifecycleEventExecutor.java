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

  private void setRemovedVnfcInstance(VNFCInstance removedVnfcInstance) {
    this.removedVnfcInstance = removedVnfcInstance;
  }
}
