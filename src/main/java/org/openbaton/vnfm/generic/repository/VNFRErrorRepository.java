package org.openbaton.vnfm.generic.repository;

import org.openbaton.vnfm.generic.model.VNFRErrorStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface VNFRErrorRepository extends CrudRepository<VNFRErrorStatus, String> {
  VNFRErrorStatus findFirstByVnfrId(String vnfrId);
  void deleteByVnfrId(String vnfrId);
}
