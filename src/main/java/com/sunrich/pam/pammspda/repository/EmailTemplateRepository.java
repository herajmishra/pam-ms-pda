package com.sunrich.pam.pammspda.repository;

import com.sunrich.pam.common.domain.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

  Optional<EmailTemplate> findByCodeAndRecordStatusTrue(String code);
}
