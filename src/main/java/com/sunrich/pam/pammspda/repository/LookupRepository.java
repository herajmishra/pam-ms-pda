package com.sunrich.pam.pammspda.repository;

import com.sunrich.pam.common.domain.Lookup;
import com.sunrich.pam.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LookupRepository extends JpaRepository<Lookup, Long> {

    List<Lookup> findByGroupKeyAndStatusAndRecordStatusTrue(String groupKey, Status active);

    List<Lookup> findByGroupKeyAndOrgIdAndStatusAndRecordStatusTrue(String groupKey, Long orgId, Status active);

    List<Lookup> findByGroupKeyIn(String[] keyList);
}
