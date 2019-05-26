package com.sunrich.pam.pammspda.repository;

import com.sunrich.pam.common.domain.pda.PdaData;
import com.sunrich.pam.common.dto.pda.PdaProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PdaRepository extends JpaRepository<PdaData, Long> {

    List<PdaData> findAllByRecordStatusTrue();

    Optional<PdaData> findByIdAndRecordStatusTrue(Long id);

    Optional<PdaData> findByTypeAndRecordStatusTrue(String type);

    @Query(value = "select v.name as vesselName, v.type as vesselType, brn.name as branchName, c.name as customerName, c.address as customerAddress, pt.iso_port_code as isoPortCode, pt.description as portName," +
            " bd.bank as bankName from pda_data p inner join vessel v on (p.vessel = v.id)" +
            " inner join branch brn on (p.branch =  brn.id)" +
            " inner join customer c on (p.customer = c.id)" +
            " inner join port pt on (p.port = pt.id)" +
            " inner join bank_details bd on (p.bank = bd.id)" +
            " WHERE p.record_status = true AND p.id =:id", nativeQuery = true)
    PdaProjection findPdaData(Long id);

    @Query(value = "select DISTINCT brn.name as branchName, brn.org_id as orgId from branch brn WHERE brn.record_status = true AND brn.id =:id", nativeQuery = true)
    PdaProjection getProjection(Long id);

    @Query(value = " select DISTINCT dischargept.description as dischargePortName from pda_data p inner join port dischargept on (p.discharge_port = dischargept.id) WHERE p.record_status = true AND p.discharge_port =:dischargePort", nativeQuery = true)
    PdaProjection getDischargePortName(String dischargePort);

    @Query(value = "select DISTINCT lastpt.description as lastPortName from pda_data p inner join port lastpt on (p.last_port = lastpt.id) WHERE p.record_status = true AND p.last_port =:lastPort", nativeQuery = true)
    PdaProjection getLastPortName(String lastPort);

    @Query(value = "select DISTINCT loadpt.description as loadPortName from pda_data p inner join port loadpt on (p.load_port = loadpt.id) WHERE p.record_status = true AND p.load_port =:loadPort", nativeQuery = true)
    PdaProjection getLoadPortName(String loadPort);

    @Query(value = "select DISTINCT npt.description as nextPortName from pda_data p inner join port npt on (p.next_port = npt.id) WHERE p.record_status = true AND p.next_port =:nextPort", nativeQuery = true)
    PdaProjection getNextPortName(String nextPort);

    @Query(value = "select DISTINCT b.description as berthName from pda_data p inner join berth b on (p.berth = b.id) WHERE p.record_status = true AND p.berth =:berth", nativeQuery = true)
    PdaProjection getBerthName(String berth);

    PdaData findCustomerByIdAndRecordStatusTrue(Long id);

    Optional<PdaData> findByIdNotAndCustomerAndPortAndVesselAndEtaAndPdaNoNotIgnoreCaseContainingAndRecordStatusTrue(long l, Long customer, Long port, Long vessel, String eta, String pdaNo);

    List<PdaData> findByPdaStatusAndRecordStatusTrue(String pdaStatus);
}
