package com.sunrich.pam.pammspda.repository;

import com.sunrich.pam.common.constants.Constants;
import com.sunrich.pam.common.domain.pda.PdaServices;
import com.sunrich.pam.common.dto.pda.PdaServiceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PdaServicesRepository extends JpaRepository<PdaServices, Long>, PdaServicesRepositoryCustom {

    Optional<PdaServices> findByIdAndRecordStatusTrue(Long id);

    Optional<PdaServices> findByPdaIdNotAndServiceIdAndRecordStatusTrue(Long id, Long serviceId);

    List<PdaServices> findAllByRecordStatusTrueOrderByIdDesc();

    List<PdaServices> findAllByPdaIdAndRecordStatusTrue(Long id);

  @Query(value = "select sd.code as serviceCode, sd.description as description ,l.value as chargeCategory from pda_services ps inner join service_details sd on ps.service_id = sd.id inner join lookup l on sd.charge_category = l.code where ps.service_id = :service_id and ps.pda_id = :pdaId", nativeQuery = true)
  List<PdaServiceProjection> findServiceCategoryAndServiceDescription(@Param("service_id") Long id, @Param("pdaId") Long pdaId);

  List<PdaServices> findByPdaIdAndTypeAndRecordStatusTrue(Long pdaId, Constants.PdaType type);

  List<PdaServices> findByPdaIdAndTypeAndServiceGroupAndRecordStatusTrue(Long id, Constants.PdaType type, Integer group);

  List<PdaServices> findAllByTypeAndPdaIdAndStatusAndRecordStatusTrue(Constants.PdaType type, Long pdaId, String status);

  @Query(value = "select sum(amount_requested) as amtRequested from branch_requisition where service_id = :serviceId and is_branch_approved = true and record_status = true", nativeQuery = true)
  PdaServiceProjection getRequestedAmount(Long serviceId);

  @Query(value = "select sum(amount_approved) as amtApproved from branch_requisition where service_id = :serviceId and is_approved = true and record_status = true", nativeQuery = true)
  PdaServiceProjection getApprovedAmount(Long serviceId);
}
