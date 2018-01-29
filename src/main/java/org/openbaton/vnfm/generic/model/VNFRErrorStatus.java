package org.openbaton.vnfm.generic.model;

import java.util.Set;
import javax.persistence.*;
import org.openbaton.catalogue.mano.common.Event;

@Entity
public class VNFRErrorStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String vnfrId;

  private String Event;

  private Integer Script;
  @ElementCollection(fetch = FetchType.EAGER)
  private Set<String> vnfciId;

  public VNFRErrorStatus() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEvent() {
    return Event;
  }

  public void setEvent(String event) {
    Event = event;
  }

  public Set<String> getVnfciId() {
    return vnfciId;
  }

  public void setVnfciId(Set<String> vnfciId) {
    this.vnfciId = vnfciId;
  }

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
    return "VNFRErrorStatus{" +
        "id='" + id + '\'' +
        ", vnfrId='" + vnfrId + '\'' +
        ", Event='" + Event + '\'' +
        ", Script=" + Script +
        ", vnfciId=" + vnfciId +
        '}';
  }
}
