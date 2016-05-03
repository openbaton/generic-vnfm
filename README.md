# Generic VNF Manager
[![Build Status](https://travis-ci.org/openbaton/generic-vnfm.svg?branch=develop)](https://travis-ci.org/openbaton/generic-vnfm)

The Generic VNFManager is an implementation following the [ETSI MANO][nfv-mano] specifications. t works as intermediate component between the NFVO and the VNFs, particularly the Virtual Machines on top of which the VNF software is installed. In order to complete the lifecycle of a VNF, it interoperates with the Element Management System (EMS) acting as an agent inside the VMs and executing scripts containeed in the vnf package.
This VNFM may be assigned the management of a single VNF instance, or the management of multiple VNF instances of the same type or of different types.

The Generic VNFManager is supposed to be used for any type of VNF that follows some convetions regarding:

* VMs deployment
* script execution order
* VMs termination

Please refer to [our documentation][generic-vnfm] for more details about this project. Below you find just few details about this project. 

## VMs deployment

The allocation of resources (VMs) are requested by the VNFManager to the NFVO. Before that all the VNFManagers need to request whenever the resources are available on the selected PoP. This is done by the GRANT_OPERATION message and it is executed by all the VNFManagers. The Generic VNFM sends the ALLOCATE_RESOURCES message as well. If the GRANT_OPERATION message is returned, than it means that there are enough resources, if not an ERROR message will be sent. After the GRANT_OPERATION message it is possible to send the ALLOCATE_RESOURCE message. This message will create all the resources and than, if no errors occured, return the ALLOCATE_RESOURCE message to the VNFManager. after that point the VMs are created and the VNFRecord is filled with values, such as ips, that can be found directly in the VirtalNetworkFunctionRecord->VirtualDeploymentUnit->VNFCInstance object. 

## Script Execution Costraints

During the INSTANTIATE and the MODIFY operations, scripts are executed in the VMs. The ordering of this scripts is defined in the NetworkServiceDescriptor from which the NetworkServiceRecord was created, in particural into the VirtalNetworkFunctionRecord->LifecycleEvents (see VNFD doc). The available parameters are defined into the VirtalNetworkFunctionDescriptor fields:

* provides
* configurations

In the INSTANTIATE scripts, the parameters defined into these two fields are then available as environment variables into the script exactly as defined.

In the MODIFY scripts, the INSTANTIATE parameters are still available but plus there are environment variables that come from a VNFDependency. These kind of parameters are defined into the _requires_ and the VNFDependency->parameters fields, and are then available as $*type_of_vnf_source*.*name_of_parameter*


## VMs termination

As for VMs deployment, VMs termination is done by the NFVO. Specific scripts can be run before termination by putting them under the RELEASE_REOSURCES lifecycel event.

## License

Copyright (c) 2015-2016 Fraunhofer FOKUS. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Supported by
Open Baton is a project developed by Fraunhofer FOKUS and TU Berlin. It is supported by different European publicly funded projects: 

* [NUBOMEDIA][nubomedia]
* [Mobile Cloud Networking][mcn]
* [CogNet][cognet]

<!---
References
-->
[generic-vnfm]: http://openbaton.github.io/documentation/vnfm-generic/
[nubomedia]: https://www.nubomedia.eu/
[mcn]: http://mobile-cloud-networking.eu/site/
[nfv-mano]: http://www.etsi.org/deliver/etsi_gs/NFV-MAN/001_099/001/01.01.01_60/gs_NFV-MAN001v010101p.pdf
[cognet]: http://www.cognet.5g-ppp.eu/cognet-in-5gpp/
