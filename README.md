# VNFManager Generic

The Generic VNFManager is an implementation following the [ETSI MANO][nfv-mano] specifications. For that reason is highly tied to the EMS.

The Generic VNFManager handles communication with the NFVO and with EMS. The communication between NFVO and the EMS is done through Stomp protocol, in particural JMS. The sequece diagram following explains the communication messages.

The Generic VNFManager is supposed to be used for any type of VNF that follows some convetions regarding:

* VMs deployment
* script execution order
* VMs termination


### VMs deployment

As seen in the previous picture, the allocation of resources (VMs) are requested by the VNFManager to the NFVO. Before that all the VNFManagers need to request whenever the resources are available on the selected PoP. This is done by the GRANT_OPERATION message and it is executed by all the VNFManagers. The Generic VNFM sends the ALLOCATE_RESOURCES message as well. If the GRANT_OPERATION message is returned, than it means that there are enough resources, if not an ERROR message will be sent. After the GRANT_OPERATION message it is possible to send the ALLOCATE_RESOURCE message. This message will create all the resources and than, if no errors occured, return the ALLOCATE_RESOURCE message to the VNFManager. after that point the VMs are created and the VNFRecord is filled with values, such as ips, that can be found directly in the VirtalNetworkFunctionRecord->VirtualDeploymentUnit->VNFCInstance object. 

### Script Execution Costraints

During the INSTANTIATE and the MODIFY operations, scripts are executed in the VMs. The ordering of this scripts is defined in the NetworkServiceDescriptor from which the NetworkServiceRecord was created, in particural into the VirtalNetworkFunctionRecord->LifecycleEvents (see VNFD doc). The available parameters are defined into the VirtalNetworkFunctionDescriptor fields:

* provides
* configurations

In the INSTANTIATE scripts, the parameters defined into these two fields are then available as environment variables into the script exactly as defined.

In the MODIFY scripts, the INSTANTIATE parameters are still available but plus there are environment variables that come from a VNFDependency. These kind of parameters are defined into the _requires_ and the VNFDependency->parameters fields, and are then available as $*type_of_vnf_source*.*name_of_parameter*


### VMs termination

As for VMs deployment, VMs termination is done by the NFVO. Specific scripts can be run before termination by putting them under the RELEASE_REOSURCES lifecycel event.

### Supported by
Open Baton is a project developed by Fraunhofer FOKUS and TU Berlin. It is supported by different European publicly funded projects: 

* [NUBOMEDIA][nubomedia]
* [Mobile Cloud Networking][mcn]
* [CogNet][cognet]

[nubomedia]: https://www.nubomedia.eu/
[mcn]: http://mobile-cloud-networking.eu/site/
[cognet]: http://www.cognet.5g-ppp.eu/cognet-in-5gpp/

<!---
References
-->

[nfv-mano]: http://www.etsi.org/deliver/etsi_gs/NFV-MAN/001_099/001/01.01.01_60/gs_NFV-MAN001v010101p.pdf
