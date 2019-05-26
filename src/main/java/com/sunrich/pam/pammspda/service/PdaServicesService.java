package com.sunrich.pam.pammspda.service;

import com.sunrich.pam.common.constants.Constants;
import com.sunrich.pam.common.domain.pda.PdaServices;
import com.sunrich.pam.common.dto.pda.PdaServiceProjection;
import com.sunrich.pam.common.dto.pda.PdaServicesDto;
import com.sunrich.pam.common.enums.PdaStatus;
import com.sunrich.pam.common.exception.BaseRuntimeException;
import com.sunrich.pam.common.exception.ErrorCodes;
import com.sunrich.pam.pammspda.repository.PdaServicesRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PdaServicesService {

    private PdaServicesRepository pdaServicesRepository;
    private ModelMapper modelMapper;
    @PersistenceContext
    EntityManager entityManager;

    public PdaServicesService(PdaServicesRepository pdaServicesRepository, ModelMapper modelMapper) {
        this.pdaServicesRepository = pdaServicesRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Used to save or update the PdaServices
     *
     * @param payload - domain object
     * @param status
     * @return - the saved/updated PdaServices object
     */
    public PdaServicesDto saveOrUpdate(PdaServicesDto payload, String status) {
        PdaServices pdaServices = new PdaServices();

        if (payload.getId() != null) {
            pdaServices = findEntityById(payload.getId());
        }
        modelMapper.map(payload, pdaServices);

        pdaServices.setRecordStatus(true);
        if (pdaServices.getTaxRate() == null) {
            pdaServices.setTaxRate("0");
        }
        if (pdaServices.getTaxAmount() == null) {
            pdaServices.setTaxAmount("0");
        }

        if (payload.getType().equals(Constants.PdaType.SPDA)) {
            pdaServices.setStatus(status);
            Integer group = pdaServicesRepository.getGroup(payload.getPdaId(), status);
          if (status.equalsIgnoreCase(Constants.DRAFT_STATUS)) {
                pdaServices.setServiceGroup(group == null ? 1 : group);
            } else {
                pdaServices.setServiceGroup(group == null ? pdaServicesRepository.getMaxGroup(payload.getPdaId()) + 1 : group);
            }
        }

        PdaServices savedPdaServices = pdaServicesRepository.save(pdaServices);

        return modelMapper.map(savedPdaServices, PdaServicesDto.class);
    }

    /**
     * Used to check pdaServices exist or not
     *
     * @param pdaServicesDTO - pdaServices identifier
     * @return - return pdaServices exist or not
     */
    public boolean isPdaServicesExist(PdaServicesDto pdaServicesDTO) {
        Long id = pdaServicesDTO.getPdaId();
        Optional<PdaServices> optionalPdaServices = pdaServicesRepository.findByPdaIdNotAndServiceIdAndRecordStatusTrue(id == null ? 0 : id, pdaServicesDTO.getServiceId());
        return optionalPdaServices.isPresent();
    }

    /**
     * Used to get the PdaServices entity by id
     *
     * @param id - pdaServices identifier
     * @return - pdaServices object
     */
    private PdaServices findEntityById(Long id) {
        Optional<PdaServices> optionalPdaServices = pdaServicesRepository.findByIdAndRecordStatusTrue(id);
        if (!optionalPdaServices.isPresent()) {
            throw new BaseRuntimeException(ErrorCodes.PDA_SERVICE_NOT_FOUND, "PdaServices not found!");
        }
        return optionalPdaServices.get();
    }

    /**
     * Used to get the pdaServices by id
     *
     * @param id - pdaServices identifier
     * @return - PdaServicesDto
     */
    public PdaServicesDto findById(Long id) {
        PdaServices pdaServices = findEntityById(id);
        return modelMapper.map(pdaServices, PdaServicesDto.class);
    }

    /**
     * Used to get the list of pdaServices
     *
     * @return - list of pdaServices
     */
    public List<PdaServicesDto> findAll() {
        List<PdaServices> pdaServicesList = pdaServicesRepository.findAllByRecordStatusTrueOrderByIdDesc();
        return pdaServicesList.stream()
                .map(pdaServices -> modelMapper.map(pdaServices, PdaServicesDto.class))
                .collect(Collectors.toList());
    }

    /**
     * Used to logically delete the pdaServices by updating recordStatus flag to false
     *
     * @param ids - pdaServices identifier
     * @return - removed pdaServices identifier
     */
    @Transactional
    public void delete(Long[] ids) {
      if (ids.length != 0 ){
        Query query = entityManager.createNativeQuery("UPDATE pda_services ps SET record_status = false WHERE ps.id IN (:ids)");
        query.setParameter("ids", Arrays.asList(ids));
        query.executeUpdate();
      }
    }

    public List<PdaServicesDto> saveOrUpdateServices(List<PdaServicesDto> payload) {
        List<PdaServicesDto> pdaServicesDtoList = new ArrayList<>();

        for (PdaServicesDto pdaService : pdaServicesDtoList) {

        }

        return pdaServicesDtoList;
    }

    /**
     * Used to get list of pda service using pda id
     * @param id -pda identifier
     * @return -list of pda services
     */
    public List<PdaServicesDto> findByPdaId(Long id, Integer group, Constants.PdaType type) {
        List<PdaServices> pdaServiceList = group == null ? pdaServicesRepository.findByPdaIdAndTypeAndRecordStatusTrue(id, type) : pdaServicesRepository.findByPdaIdAndTypeAndServiceGroupAndRecordStatusTrue(id, type, group);
        List<PdaServicesDto> pdaServiceDtoList = new ArrayList<>();
        for (PdaServices pdaService : pdaServiceList){

            PdaServicesDto pdaServicesDTO = modelMapper.map(pdaService,PdaServicesDto.class);
          PdaServiceProjection pdaServiceProjection = pdaServicesRepository.findServiceCategoryAndServiceDescription(pdaServicesDTO.getServiceId(), pdaServicesDTO.getPdaId()).get(0);

            pdaServicesDTO.setChargeCategory(pdaServiceProjection.getChargeCategory());
            pdaServicesDTO.setDescription(pdaServiceProjection.getDescription());
            pdaServicesDTO.setServiceCode(pdaServiceProjection.getServiceCode());

            pdaServiceDtoList.add(pdaServicesDTO);

        }
        return pdaServiceDtoList;
    }

  /**
   * Used to get the list of pdaServices and service pda services
   *
   * @return - list of pdaServices and service pda services
   */
  public List<PdaServicesDto> findAllPdaServicesAndServicePdaServices(Long pdaId) {

    List<PdaServices> pdaServiceList = pdaServicesRepository.findByPdaIdAndTypeAndRecordStatusTrue(pdaId, Constants.PdaType.PDA);
    List<PdaServicesDto> pdaServicesDtoList = pdaServiceList.stream()
            .map(pdaServices -> modelMapper.map(pdaServices, PdaServicesDto.class))
            .collect(Collectors.toList());

    List<PdaServices> servicePdaServiceList = pdaServicesRepository.findAllByTypeAndPdaIdAndStatusAndRecordStatusTrue(Constants.PdaType.SPDA, pdaId, String.valueOf(PdaStatus.ACC));
    List<PdaServicesDto> servicePdaServicesDtoList = servicePdaServiceList.stream()
            .map(servicePdaServices -> modelMapper.map(servicePdaServices, PdaServicesDto.class))
            .collect(Collectors.toList());

    List<PdaServicesDto> finalList = new ArrayList<>();
    List<PdaServicesDto> finalListx = new ArrayList<>();

    finalList.addAll(pdaServicesDtoList);
    finalList.addAll(servicePdaServicesDtoList);

    for (PdaServicesDto pdaServiceDto : finalList) {
      PdaServiceProjection projectionForRequestedAmt = pdaServicesRepository.getRequestedAmount(pdaServiceDto.getId());
      PdaServiceProjection projectionForApprovedAmt = pdaServicesRepository.getApprovedAmount(pdaServiceDto.getId());
      if (projectionForRequestedAmt != null) {
        pdaServiceDto.setAmtRequested(projectionForRequestedAmt.getAmtRequested());
      }
      if (projectionForApprovedAmt != null) {
        pdaServiceDto.setAmtApproved(projectionForApprovedAmt.getAmtApproved());
      }
      finalListx.add(pdaServiceDto);
    }
    return finalListx;
  }
}
