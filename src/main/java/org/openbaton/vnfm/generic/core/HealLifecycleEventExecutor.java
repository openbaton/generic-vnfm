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
