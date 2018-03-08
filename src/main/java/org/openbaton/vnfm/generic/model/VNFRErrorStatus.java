package org.openbaton.vnfm.generic.model;

import java.util.Set;
import javax.persistence.*;
import org.openbaton.catalogue.mano.common.Event;
import org.openbaton.catalogue.nfvo.Action;

@Entity
public class VNFRErrorStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String vnfrId;

  @Enumerated(EnumType.STRING)
  private Action action;

  @Enumerated(EnumType.STRING)
  private Event event;

  private Integer Script;

 // @ElementCollection(fetch = FetchType.EAGER)
//  private Set<String> vnfciId;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

 // public Set<String> getVnfciId() {
 //   return vnfciId;
 // }

 // public void setVnfciId(Set<String> vnfciId) {
//    this.vnfciId = vnfciId;
//  }

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
        + "id='"
        + id
        + '\''
        + ", vnfrId='"
        + vnfrId
        + '\''
        + ", action='"
        + action
        + '\''
        + ", Script="
        + Script
        + ", vnfciId="
 //       + vnfciId
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
