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

package org.openbaton.vnfm.generic.model;

import javax.persistence.*;
import org.openbaton.catalogue.mano.common.Event;
import org.openbaton.catalogue.util.BaseEntity;

@Entity
public class VNFRErrorStatus extends BaseEntity {

  private String vnfrId;

  @Enumerated(EnumType.STRING)
  private Event event;

  private Integer scriptIndex;

  public VNFRErrorStatus() {}

  public VNFRErrorStatus(String vnfrId, Event event, Integer script) {
    this.vnfrId = vnfrId;
    this.event = event;
    this.scriptIndex = script;
  }

  public Integer getScriptIndex() {
    return scriptIndex;
  }

  public void setScriptIndex(Integer scriptIndex) {
    this.scriptIndex = scriptIndex;
  }

  public String getVnfrId() {
    return vnfrId;
  }

  public void setVnfrId(String vnfrId) {
    this.vnfrId = vnfrId;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  @Override
  public String toString() {
    return "VNFRErrorStatus{"
        + "vnfrId='"
        + vnfrId
        + '\''
        + ", event="
        + event
        + ", scriptIndex="
        + scriptIndex
        + '}';
  }
}
