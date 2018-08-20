<img src="https://raw.githubusercontent.com/openbaton/openbaton.github.io/master/images/openBaton.png" width="250"/>
  
  Copyright © 2015-2016 [Open Baton](http://openbaton.org). 
  Licensed under [Apache v2 License](http://www.apache.org/licenses/LICENSE-2.0).
  
[![Build Status](https://travis-ci.org/openbaton/generic-vnfm.svg?branch=develop)](https://travis-ci.org/openbaton/generic-vnfm)
  
# Generic VNF Manager

The Generic VNFManager is an implementation of a VNF Manager following the ETSI MANO specifications. It works as intermediate component between the NFVO and the VNFs, particularly the Virtual Machines on top of which the VNF software is installed. In order to complete the lifecycle of a VNF, it interoperates with the Element Management System (EMS) acting as an agent inside the VMs and executing scripts containeed in the vnf package.
This VNFM may be assigned the management of a single VNF instance, or the management of multiple VNF instances of the same type or of different types.

The Generic VNFManager is supposed to be used for any type of VNF that follows some conventions regarding:

* VMs deployment
* scriptIndex execution
* VMs termination

Please refer to [our documentation][generic-vnfm] for more details about this project.
 
## Technical Requirements

* openjdk 7+ or oracle JDK 7+
* a [NFVO](https://github.com/openbaton/NFVO) running instance
* The VMs need to have access to the openbaton repository (internet) thus to be able to dynamically download the [EMS](https://github.com/openbaton/ems)

## Getting Started

## Setup environment

Once you are in the unpacked folder of the generic-vnfm, execute the following commands:

```bash
sudo mkdir -p /etc/openbaton
sudo chown -R $USER: /etc/openbaton/
cp user-data.sh /etc/openbaton/openbaton-vnfm-generic-user-data.sh
```

## Start the Generic VNFM

```bash
./bin/openbaton-vnfm-generic start
```

**Note**: you can check out the logs at /var/log/openbaton/generic-vnfm.log

[generic-vnfm]:http://openbaton.github.io/documentation/nfvo-installation-deb/