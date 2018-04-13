/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.openbaton.vnfm.generic.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;
import org.mockito.Mock;
import org.openbaton.catalogue.mano.common.*;
import org.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.Location;
import org.openbaton.catalogue.nfvo.images.NFVImage;
import org.openbaton.catalogue.nfvo.networks.Network;
import org.openbaton.catalogue.nfvo.viminstances.BaseVimInstance;
import org.openbaton.catalogue.nfvo.viminstances.OpenstackVimInstance;
import org.openbaton.vnfm.generic.repository.VNFRErrorRepository;
import org.openbaton.vnfm.generic.utils.LogUtils;

public class LifecycleEventExecutorTest {

  @Mock private ElementManagementSystem ems;
  @Mock private LogUtils logUtils;
  @Mock private VNFRErrorRepository vnfrErrorRepository;

  @Test
  public void testGetVnfcInstances() {
    VirtualNetworkFunctionRecord vnfr = createVirtualNetworkFunctionRecord();
    BaseVimInstance vimInstance = createVimInstance();
    for (int i = 0; i < 3; i++) {
      vnfr.getVdu().add(createVDU(i, vimInstance));
    }
    LifecycleEvent le = createLifecycleEvent(Event.INSTANTIATE);

    LifecycleEventExecutor lifecycleEventExecutor = new GeneralLifecycleEventExecutor(le, vnfr);
    setLifecycleEventExecutorDependencies(lifecycleEventExecutor);

    assertEquals(
        "Number of vnfc instances is not 3", 3, lifecycleEventExecutor.getVnfcInstances().size());
  }

  private LifecycleEvent createLifecycleEvent(Event event) {
    LifecycleEvent lifecycleEvent = new LifecycleEvent();
    lifecycleEvent.setEvent(event);
    lifecycleEvent.setLifecycle_events(
        new ArrayList<String>() {
          {
            add("script1");
            add("script2");
            add("script3");
          }
        });
    return lifecycleEvent;
  }

  private VirtualNetworkFunctionRecord createVirtualNetworkFunctionRecord() {
    VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = new VirtualNetworkFunctionRecord();
    virtualNetworkFunctionRecord.setName("mocked_vnfr");
    virtualNetworkFunctionRecord.setVdu(new HashSet<>());
    return virtualNetworkFunctionRecord;
  }

  private static OpenstackVimInstance createVimInstance() {
    OpenstackVimInstance vimInstance = new OpenstackVimInstance();
    vimInstance.setId("vim_instance_id");
    vimInstance.setName("vim_instance");
    vimInstance.setAuthUrl("http://auth.url.hello");
    vimInstance.setTenant("tenant");
    vimInstance.setUsername("username");
    vimInstance.setPassword("password");
    vimInstance.setType("test");
    Location location = new Location();
    location.setName("Location");
    location.setLatitude("8463947");
    location.setLongitude("-857638");
    vimInstance.setLocation(location);
    vimInstance.setNetworks(
        new HashSet<Network>() {
          {
            Network network = new Network();
            network.setExtId("ext_id");
            network.setName("network_name");
            add(network);
          }
        });
    vimInstance.setFlavours(
        new HashSet<DeploymentFlavour>() {
          {
            DeploymentFlavour deploymentFlavour = new DeploymentFlavour();
            deploymentFlavour.setExtId("ext_id_1");
            deploymentFlavour.setFlavour_key("flavor_name");
            add(deploymentFlavour);

            deploymentFlavour = new DeploymentFlavour();
            deploymentFlavour.setExtId("ext_id_2");
            deploymentFlavour.setFlavour_key("m1.tiny");
            add(deploymentFlavour);
          }
        });
    vimInstance.setImages(
        new HashSet<NFVImage>() {
          {
            NFVImage image = new NFVImage();
            image.setExtId("ext_id_1");
            image.setName("ubuntu-14.04-server-cloudimg-amd64-disk1");
            add(image);

            image = new NFVImage();
            image.setExtId("ext_id_2");
            image.setName("image_name_1");
            add(image);
          }
        });
    return vimInstance;
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
    lifecycleEventExecutor.setEms(ems);
    lifecycleEventExecutor.setLogUtils(logUtils);
    lifecycleEventExecutor.setVnfrErrorRepository(vnfrErrorRepository);
  }
}
