package com.sunrich.pam.pammspda.repository;

import com.sunrich.pam.common.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>, CustomerRepositoryCustom {

  Optional<Customer> findByIdAndRecordStatusTrue(Long id);
}
