package com.sunrich.pam.pammspda.repository;

import com.sunrich.pam.common.domain.BankDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<BankDetails, Long> {

    List<BankDetails> findByOrgIdAndRecordStatusTrue(Long orgId);

    List<BankDetails> findByRecordStatusTrue();

    Optional<BankDetails> findByIdAndRecordStatusTrue(Long id);
}
