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

import java.util.ArrayList;
import java.util.List;
import org.openbaton.catalogue.mano.common.Event;
import org.openbaton.catalogue.mano.common.LifecycleEvent;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.common.vnfm_sdk.utils.VnfmUtils;
import org.openbaton.vnfm.generic.repository.VNFRErrorRepository;
import org.openbaton.vnfm.generic.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** Created by lto on 15/09/15. */
@Service
@Transactional
public class LifeCycleManagement {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired private ElementManagementSystem ems;
  @Autowired private LogUtils logUtils;
  @Autowired private VNFRErrorRepository vnfrErrorRepository;

  public Iterable<String> executeScriptsForEvent(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Event event) throws Exception {
    //TODO make it parallel
    LifecycleEvent le =
        VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), event);
    LifecycleEventExecutor lifecycleEventExecutor =
        new GeneralLifecycleEventExecutor(le, virtualNetworkFunctionRecord);
    setLifecycleEventExecutorDependencies(lifecycleEventExecutor);
    return lifecycleEventExecutor.execute();
  }

  public Iterable<String> executeScriptsForEvent(
      VirtualNetworkFunctionRecord vnfr, Event event, VNFRecordDependency dependency)
      throws Exception {

    LifecycleEvent le = VnfmUtils.getLifecycleEvent(vnfr.getLifecycle_event(), event);
    ConfigureLifecycleEventExecutor configureLifecycleEventExecutor =
        new ConfigureLifecycleEventExecutor(le, vnfr, dependency);
    setLifecycleEventExecutorDependencies(configureLifecycleEventExecutor);
    return configureLifecycleEventExecutor.execute();
  }

  public Iterable<String> executeScriptsForEvent(
      VirtualNetworkFunctionRecord vnfr,
      VNFCInstance vnfcInstance,
      Event event,
      VNFRecordDependency dependency)
      throws Exception {
    LifecycleEvent le = VnfmUtils.getLifecycleEvent(vnfr.getLifecycle_event(), event);
    ConfigureLifecycleEventExecutor configureLifecycleEventExecutor =
        new ConfigureLifecycleEventExecutor(le, vnfr, dependency);
    setLifecycleEventExecutorDependencies(configureLifecycleEventExecutor);
    return configureLifecycleEventExecutor.executeOn(vnfcInstance);
  }

  public Iterable<String> executeScriptsForEvent(
      VirtualNetworkFunctionRecord vnfr, VNFCInstance vnfcInstance, Event event, String cause)
      throws Exception {
    LifecycleEvent le = VnfmUtils.getLifecycleEvent(vnfr.getLifecycle_event(), event);
    HealLifecycleEventExecutor healLifecycleEventExecutor =
        new HealLifecycleEventExecutor(le, vnfr, cause);
    setLifecycleEventExecutorDependencies(healLifecycleEventExecutor);
    return healLifecycleEventExecutor.executeOn(vnfcInstance);
  }

  public Iterable<String> executeScriptsForEvent(
      VirtualNetworkFunctionRecord vnfr, VNFCInstance vnfcInstance, Event event) throws Exception {

    LifecycleEvent le = VnfmUtils.getLifecycleEvent(vnfr.getLifecycle_event(), event);
    GeneralLifecycleEventExecutor generalLifecycleEventExecutor =
        new GeneralLifecycleEventExecutor(le, vnfr);
    setLifecycleEventExecutorDependencies(generalLifecycleEventExecutor);
    return generalLifecycleEventExecutor.executeOn(vnfcInstance);
  }

  public Iterable<? extends String> executeScriptsForEventOnVnfr(
      VirtualNetworkFunctionRecord vnfr, VNFCInstance vnfcInstanceRemote, Event event)
      throws Exception {
    LifecycleEvent le = VnfmUtils.getLifecycleEvent(vnfr.getLifecycle_event(), event);
    ScaleInLifecycleEventExecutor scaleInLifecycleEventExecutor =
        new ScaleInLifecycleEventExecutor(le, vnfr, vnfcInstanceRemote);
    setLifecycleEventExecutorDependencies(scaleInLifecycleEventExecutor);
    return scaleInLifecycleEventExecutor.execute();
  }

  public Iterable<String> executeScriptsForEvent(
      VirtualNetworkFunctionRecord vnfr,
      Event erroredEvent,
      VNFCInstance vnfcInstance,
      Integer scriptId,
      VNFRecordDependency dependency)
      throws Exception { // Invoked by Resume method
    LifecycleEvent le = VnfmUtils.getLifecycleEvent(vnfr.getLifecycle_event(), erroredEvent);
    List<String> scripts = new ArrayList<>();
    for (String script : le.getLifecycle_events()) {
      if (le.getLifecycle_events().indexOf(script) >= scriptId) {
        scripts.add(script);
      }
    }
    LifecycleEventExecutor lifecycleEventExecutor;
    if (erroredEvent.ordinal() == Event.CONFIGURE.ordinal()) {
      lifecycleEventExecutor = new ConfigureLifecycleEventExecutor(le, vnfr, dependency);
      lifecycleEventExecutor.setScripts(scripts);
    } else {
      lifecycleEventExecutor = new GeneralLifecycleEventExecutor(le, vnfr);
      lifecycleEventExecutor.setScripts(scripts);
    }
    setLifecycleEventExecutorDependencies(lifecycleEventExecutor);
    return lifecycleEventExecutor.executeOn(vnfcInstance);
  }

  private void setLifecycleEventExecutorDependencies(
      LifecycleEventExecutor lifecycleEventExecutor) {
    lifecycleEventExecutor.setEms(ems);
    lifecycleEventExecutor.setLogUtils(logUtils);
    lifecycleEventExecutor.setVnfrErrorRepository(vnfrErrorRepository);
  }
}
