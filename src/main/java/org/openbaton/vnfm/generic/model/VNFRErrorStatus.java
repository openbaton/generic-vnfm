package org.openbaton.vnfm.generic.model;

import javax.persistence.*;
import org.openbaton.catalogue.mano.common.Event;

@Entity
public class VNFRErrorStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

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

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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
        + "id="
        + id
        + ", vnfrId='"
        + vnfrId
        + '\''
        + ", event="
        + event
        + ", scriptIndex="
        + scriptIndex
        + '}';
  }
}
