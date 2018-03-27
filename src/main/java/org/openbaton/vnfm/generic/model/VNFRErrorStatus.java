package org.openbaton.vnfm.generic.model;

import javax.persistence.*;
import org.openbaton.catalogue.mano.common.Event;
import org.openbaton.catalogue.nfvo.Action;
import org.openbaton.catalogue.util.BaseEntity;

@Entity
public class VNFRErrorStatus extends BaseEntity {

  private String vnfrId;

  @Enumerated(EnumType.STRING)
  private Action action;

  @Enumerated(EnumType.STRING)
  private Event event;

  private Integer Script;

  public Integer getScript() {
    return Script;
  }

  public void setScript(Integer script) {
    Script = script;
  }

  public String getVnfrId() {
    return vnfrId;
  }

  public void setVnfrId(String vnfrId) {
    this.vnfrId = vnfrId;
  }

  @Override
  public String toString() {
    return "VNFRErrorStatus{"
        + "vnfrId='"
        + vnfrId
        + '\''
        + ", action='"
        + action
        + '\''
        + ", Script="
        + Script
        + '}';
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }
}
