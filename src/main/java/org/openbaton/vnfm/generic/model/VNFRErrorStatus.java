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
