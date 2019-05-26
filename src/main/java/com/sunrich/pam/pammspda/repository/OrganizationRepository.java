package com.sunrich.pam.pammspda.repository;

import com.sunrich.pam.common.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
  Optional<Organization> findByIdAndRecordStatusTrue(Long id);
}
