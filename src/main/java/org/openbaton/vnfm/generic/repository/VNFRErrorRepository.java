package org.openbaton.vnfm.generic.repository;

import org.openbaton.vnfm.generic.model.VNFRErrorStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VNFRErrorRepository extends CrudRepository<VNFRErrorStatus,String> {
  VNFRErrorStatus findFirstByVnfrId(String vnfrId);
}
