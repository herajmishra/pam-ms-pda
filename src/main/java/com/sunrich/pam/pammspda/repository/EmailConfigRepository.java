package com.sunrich.pam.pammspda.repository;

import com.sunrich.pam.common.domain.EmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailConfigRepository extends JpaRepository<EmailConfig, Long> {

  Optional<EmailConfig> findByIdAndRecordStatusTrue(Long id);
}
