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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openbaton.catalogue.mano.common.*;
import org.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.viminstances.BaseVimInstance;
import org.openbaton.catalogue.nfvo.viminstances.OpenstackVimInstance;
import org.openbaton.vnfm.generic.repository.VNFRErrorRepository;
import org.openbaton.vnfm.generic.utils.LogUtils;

@RunWith(MockitoJUnitRunner.class)
public class LifecycleEventExecutorTest {

  @Mock private ElementManagementSystem emsMock;
  @Mock private LogUtils logUtilsMock;
  @Mock private VNFRErrorRepository vnfrErrorRepositoryMock;

  @Test
  public void testGetVnfcInstances() {
    VirtualNetworkFunctionRecord vnfr = createVirtualNetworkFunctionRecord();
    for (int i = 0; i < 3; i++) {
      vnfr.getVdu().add(createVDU(i, new OpenstackVimInstance()));
    }
    LifecycleEvent le = createLifecycleEvent(Event.INSTANTIATE);
    le.setLifecycle_events(
        new ArrayList<String>() {
          {
            add("script1");
          }
        });

    LifecycleEventExecutor lifecycleEventExecutor = new GeneralLifecycleEventExecutor(le, vnfr);
    setLifecycleEventExecutorDependencies(lifecycleEventExecutor);

    assertEquals(
        "Number of vnfc instances is not 3", 3, lifecycleEventExecutor.getVnfcInstances().size());
  }

  @Test
  public void testGetVnfcInstancesAfterExecutionOnSingleVnfcInstance() throws Exception {
    VirtualNetworkFunctionRecord vnfr = createVirtualNetworkFunctionRecord();
    VirtualDeploymentUnit vdu = createVDU(0, new OpenstackVimInstance());
    vnfr.getVdu().add(vdu);
    LifecycleEvent le = createLifecycleEvent(Event.INSTANTIATE);
    le.setLifecycle_events(
        new ArrayList<String>() {
          {
            add("script1");
          }
        });
    when(emsMock.executeActionOnEMS(
            any(String.class),
            any(String.class),
            any(VirtualNetworkFunctionRecord.class),
            any(VNFCInstance.class)))
        .thenReturn("");
    LifecycleEventExecutor lifecycleEventExecutor = new GeneralLifecycleEventExecutor(le, vnfr);
    setLifecycleEventExecutorDependencies(lifecycleEventExecutor);
    lifecycleEventExecutor.executeOn(vdu.getVnfc_instance().iterator().next());

    assertEquals(
        "Number of vnfc instances is not 1", 1, lifecycleEventExecutor.getVnfcInstances().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void assertExceptionIfScriptsAreNull() {
    VirtualNetworkFunctionRecord vnfr = createVirtualNetworkFunctionRecord();
    for (int i = 0; i < 3; i++) {
      vnfr.getVdu().add(createVDU(i, new OpenstackVimInstance()));
    }
    LifecycleEvent le = createLifecycleEvent(Event.INSTANTIATE);

    new GeneralLifecycleEventExecutor(le, vnfr);
  }

  private LifecycleEvent createLifecycleEvent(Event event) {
    LifecycleEvent lifecycleEvent = new LifecycleEvent();
    lifecycleEvent.setEvent(event);
    return lifecycleEvent;
  }

  private VirtualNetworkFunctionRecord createVirtualNetworkFunctionRecord() {
    VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = new VirtualNetworkFunctionRecord();
    virtualNetworkFunctionRecord.setId("123");
    virtualNetworkFunctionRecord.setName("mocked_vnfr");
    virtualNetworkFunctionRecord.setVdu(new HashSet<>());
    return virtualNetworkFunctionRecord;
  }

  private VirtualDeploymentUnit createVDU(int suffix, BaseVimInstance vimInstance) {
    VirtualDeploymentUnit vdu = new VirtualDeploymentUnit();
    vdu.setId("" + Math.random() * 100000);
    vdu.setHostname("mocked_vdu_hostname_" + suffix);
    HighAvailability highAvailability = new HighAvailability();
    highAvailability.setRedundancyScheme("1:N");
    highAvailability.setResiliencyLevel(ResiliencyLevel.ACTIVE_STANDBY_STATELESS);
    vdu.setHigh_availability(highAvailability);
    vdu.setVm_image(
        new HashSet<String>() {
          {
            add("mocked_image");
          }
        });
    vdu.setComputation_requirement("high_requirements");
    HashSet<VNFComponent> vnfComponents = new HashSet<>();
    vnfComponents.add(new VNFComponent());
    vnfComponents.add(new VNFComponent());
    vdu.setVnfc(vnfComponents);
    HashSet<VNFCInstance> vnfc_instance = new HashSet<>();
    vnfc_instance.add(new VNFCInstance());
    vdu.setVnfc_instance(vnfc_instance);
    vdu.setLifecycle_event(new HashSet<>());
    vdu.setMonitoring_parameter(new HashSet<>());
    Set<String> vimInstanceName = new LinkedHashSet<>();
    vimInstanceName.add(vimInstance.getName());
    vdu.setVimInstanceName(vimInstanceName);
    return vdu;
  }

  private void setLifecycleEventExecutorDependencies(
      LifecycleEventExecutor lifecycleEventExecutor) {
    lifecycleEventExecutor.setEms(emsMock);
    lifecycleEventExecutor.setLogUtils(logUtilsMock);
    lifecycleEventExecutor.setVnfrErrorRepository(vnfrErrorRepositoryMock);
  }
}
