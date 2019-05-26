package com.sunrich.pam.pammspda.repository;

import com.sunrich.pam.common.domain.pda.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TemplateRepository extends JpaRepository<Template,Long> {
  @Query(value = "select * from template where group_key = 'PDA_GENRIC' or (group_key = 'PDA_PORT' and port_id = :portId) or (group_key = 'PDA_CUSTOMER' and customer_id = :customerId);",nativeQuery = true)
  List<Template> findAllByKeyPortIdCustomerId(@Param("portId") Long portId, @Param("customerId") Long CustomerId);
}
