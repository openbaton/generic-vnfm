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

package org.openbaton.vnfm.generic.interfaces;

import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.common.vnfm_sdk.VnfmHelper;
import org.openbaton.common.vnfm_sdk.exception.BadFormatException;
import org.openbaton.vnfm.generic.model.EmsRegistrationUnit;

public interface EmsInterface {

  void init(String scriptPath, VnfmHelper vnfmHelper);

  EmsRegistrationUnit addRegistrationUnit(String hostname, boolean registered);

  EmsRegistrationUnit addRegistrationUnit(String hostname);

  void removeRegistrationUnit(String hostname) throws BadFormatException;

  void registerFromEms(String json) throws BadFormatException;

  String getEmsHeartbeat();

  String getEmsAutodelete();

  String getEmsVersion();

  boolean isSaveVNFRecordDependencySupported();

  void saveScriptOnEms(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Object scripts)
      throws Exception;

  void saveScriptOnEms(
      VNFCInstance vnfcInstance,
      Object scripts,
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord)
      throws Exception;

  void saveVNFRecordDependency(
      VirtualNetworkFunctionRecord vnfr,
      VNFCInstance vnfcInstance,
      VNFRecordDependency vnfRecordDependency)
      throws Exception;

  String executeActionOnEMS(
      String vduHostname,
      String command,
      VirtualNetworkFunctionRecord vnfr,
      VNFCInstance vnfcInstance)
      throws Exception;

  void removeRegistrationUnit(EmsRegistrationUnit unit);
}
