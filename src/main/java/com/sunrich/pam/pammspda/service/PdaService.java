package com.sunrich.pam.pammspda.service;

import com.sunrich.pam.common.constants.Constants;
import com.sunrich.pam.common.domain.BankDetails;
import com.sunrich.pam.common.domain.Customer;
import com.sunrich.pam.common.domain.Lookup;
import com.sunrich.pam.common.domain.Organization;
import com.sunrich.pam.common.domain.Token;
import com.sunrich.pam.common.domain.pda.AdditionalInformation;
import com.sunrich.pam.common.domain.pda.PdaData;
import com.sunrich.pam.common.domain.pda.PdaItem;
import com.sunrich.pam.common.domain.pda.PdaReport;
import com.sunrich.pam.common.domain.pda.PdaServices;
import com.sunrich.pam.common.domain.pda.Template;
import com.sunrich.pam.common.dto.BankDTO;
import com.sunrich.pam.common.dto.pda.PdaDto;
import com.sunrich.pam.common.dto.pda.PdaOnLoadDto;
import com.sunrich.pam.common.dto.pda.PdaProjection;
import com.sunrich.pam.common.dto.pda.PdaServiceSortingComparator;
import com.sunrich.pam.common.dto.pda.PdaServicesDto;
import com.sunrich.pam.common.enums.PdaStatus;
import com.sunrich.pam.common.enums.Status;
import com.sunrich.pam.common.exception.ConflictException;
import com.sunrich.pam.common.exception.ErrorCodes;
import com.sunrich.pam.common.exception.NotFoundException;
import com.sunrich.pam.common.exception.UnauthorizedException;
import com.sunrich.pam.common.exception.UnprocessableException;
import com.sunrich.pam.common.util.CommonUtil;
import com.sunrich.pam.pammspda.repository.BankRepository;
import com.sunrich.pam.pammspda.repository.CustomerRepository;
import com.sunrich.pam.pammspda.repository.LookupRepository;
import com.sunrich.pam.pammspda.repository.OrganizationRepository;
import com.sunrich.pam.pammspda.repository.PdaRepository;
import com.sunrich.pam.pammspda.repository.PdaServicesRepository;
import com.sunrich.pam.pammspda.repository.TemplateRepository;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PdaService {

    private PdaRepository pdaRepository;
    private LookupRepository lookupRepository;
    private PdaServicesRepository pdaServicesRepository;
    private BankRepository bankRepository;
    private CustomerRepository customerRepository;
    private PdaServicesService pdaServicesService;
    private ModelMapper modelMapper;
    private TemplateRepository templateRepository;
  private TokenService tokenService;
  private OrganizationRepository organizationRepository;

  public PdaService(PdaRepository pdaRepository, LookupRepository lookupRepository, PdaServicesRepository pdaServicesRepository, BankRepository bankRepository, CustomerRepository customerRepository, PdaServicesService pdaServicesService, ModelMapper modelMapper, TemplateRepository templateRepository, TokenService tokenService, OrganizationRepository organizationRepository) {
        this.pdaRepository = pdaRepository;
        this.lookupRepository = lookupRepository;
        this.pdaServicesRepository = pdaServicesRepository;
        this.bankRepository = bankRepository;
        this.customerRepository = customerRepository;
        this.pdaServicesService = pdaServicesService;
        this.modelMapper = modelMapper;
        this.templateRepository = templateRepository;
    this.tokenService = tokenService;
    this.organizationRepository = organizationRepository;
    }

    /**
     * Used to save or update the PdaData
     *
     * @param pdaDTO - domain object
     * @return - the saved/updated PdaData object
     */
    public PdaDto saveOrUpdate(PdaDto pdaDTO) throws Exception {
        PdaData pdaData = new PdaData();
        modelMapper.map(pdaDTO, pdaData);
        pdaConflictCheck(pdaDTO);

        if (pdaDTO.getId() != null) {
            pdaData = findEntityById(pdaDTO.getId());
            pdaData = verifyStatus(pdaDTO, pdaData);
        }
        pdaData.setRecordStatus(true);
        PdaData savedPdaData = pdaRepository.save(pdaData);
        //code to generate pda number
        if (pdaData.getPdaNo() == null) {
            pdaData.setPdaNo(generatePdaNumber(pdaDTO));
            savedPdaData = pdaRepository.save(pdaData);
            //update sequence in lookup
            Lookup lookup = lookupRepository.findByGroupKeyAndStatusAndRecordStatusTrue("PDA_SEQUENCE", Status.ACTIVE).get(0);
            int seqNumber = Integer.parseInt(lookup.getValue()) + 1;
            lookup.setValue(String.valueOf(seqNumber));
            lookupRepository.save(lookup);
        }
        //code to generate job number
        if (savedPdaData.getJobNo() == null && (PdaStatus.ACC.toString().equals(savedPdaData.getPdaStatus()))) {
            savedPdaData.setJobNo(generateJobNumber(savedPdaData));
            savedPdaData = pdaRepository.save(savedPdaData);
        }
        List<PdaServices> pdaServicesList = new ArrayList<>();
        if (pdaData.getOldId() != null) {
            pdaServicesList = pdaServicesRepository.findAllByPdaIdAndRecordStatusTrue(savedPdaData.getOldId());
        }

        if (!pdaServicesList.isEmpty()) {
            for (PdaServices pdaService : pdaServicesList) {
                PdaServices pdaServicesObj = new PdaServices();
                modelMapper.map(pdaService, pdaServicesObj);

                pdaServicesObj.setId(null);
                pdaServicesObj.setPdaId(savedPdaData.getId());
                pdaServicesRepository.save(pdaServicesObj);
            }
        }
        return modelMapper.map(savedPdaData, PdaDto.class);
    }

    /**
     * used to check pda is already present or not
     *
     * @param pdaDTO - domain object
     */
    private void pdaConflictCheck(PdaDto pdaDTO) {
      String pdaNo = "";
      if (pdaDTO.getPdaNo() != null) {
        pdaNo = pdaDTO.getPdaNo();
        if (pdaNo.length() == 14) {
          pdaNo = pdaNo.substring(0, 12);
        }
      }
      Optional<PdaData> pdaData = pdaRepository.findByIdNotAndCustomerAndPortAndVesselAndEtaAndPdaNoNotIgnoreCaseContainingAndRecordStatusTrue(pdaDTO.getId() == null ? 0 : pdaDTO.getId(), pdaDTO.getCustomer(), pdaDTO.getPort(), pdaDTO.getVessel(), pdaDTO.getEta(), pdaNo);
        if (pdaData.isPresent()) {
            throw new ConflictException(ErrorCodes.PDA_CONFLICT, "Pda already exist with id : " + pdaData.get().getPdaNo());
        }
    }

    /**
     * used to generate pda number
     *
     * @param pdaDTO - domain object
     * @return pda number
     */
    private String generatePdaNumber(PdaDto pdaDTO) {

        StringBuilder sb = new StringBuilder();

        LocalDate today = LocalDate.now();
        int year = today.getYear();

        String currentYear = String.valueOf(year).substring(String.valueOf(year).length() - 2, String.valueOf(year).length());

        PdaProjection pdaProjection = pdaRepository.getProjection(pdaDTO.getBranch());

        String branchName = pdaProjection.getBranchName().substring(0, 3);

        Lookup lookup = lookupRepository.findByGroupKeyAndStatusAndRecordStatusTrue("PDA_SEQUENCE", Status.ACTIVE).get(0);
        int seqNumber = Integer.parseInt(lookup.getValue()) + 1;

        StringBuilder stringbuidBuilder = new StringBuilder();

        for (int toPrepend = 5 - String.valueOf(seqNumber).length(); toPrepend > 0; toPrepend--) {
            stringbuidBuilder.append('0');
        }

        stringbuidBuilder.append(String.valueOf(seqNumber));
        String result = stringbuidBuilder.toString();

        return sb.append(Constants.PDA_CODE).append(currentYear).append(pdaProjection.getOrgId()).append(branchName).append(result).toString().toUpperCase();
    }

    /**
     * used to generate job number
     *
     * @param pdaData -entity object
     * @return job number
     */
    private String generateJobNumber(PdaData pdaData) {
        StringBuilder sb = new StringBuilder();
        //prefix
        sb.append(pdaData.getType()).append("-");
        //isoPortCode
        PdaProjection projection = pdaRepository.findPdaData(pdaData.getId());
        sb.append(projection.getIsoPortCode());
        //month and year
        LocalDate today = LocalDate.now();
        String month = String.valueOf(today.getMonthValue());
        int year = today.getYear();

        String currentYear = String.valueOf(year).substring(String.valueOf(year).length() - 2, String.valueOf(year).length());
        if (month.length() == 1) {
            sb.append("0");
        }
        sb.append(month);
        sb.append(currentYear);
        //auto-generate number
        Lookup lookup = lookupRepository.findByGroupKeyAndStatusAndRecordStatusTrue("JOB_SEQUENCE", Status.ACTIVE).get(0);
        int seqNumber = Integer.parseInt(lookup.getValue()) + 1;
        StringBuilder stringbuidBuilder = new StringBuilder();

        for (int toPrepend = 4 - String.valueOf(seqNumber).length(); toPrepend > 0; toPrepend--) {
            stringbuidBuilder.append('0');
        }
        stringbuidBuilder.append(String.valueOf(seqNumber));
        String result = stringbuidBuilder.toString();
        sb.append(result);
        return sb.toString().toUpperCase();
    }

    /**
     * Used to verify PdaStatus
     */
    private PdaData verifyStatus(PdaDto pdaDTO, PdaData pdaData) throws Exception {
        PdaData vpdaData = new PdaData();

      String status = pdaDTO.getPdaStatus();
        PdaStatus pdaStatus = PdaStatus.valueOf(status);
        modelMapper.map(pdaDTO, vpdaData);
      vpdaData.setRecordStatus(true);
        switch (pdaStatus) {
            case SUBM:
              String pdaNo = pdaData.getPdaNo();
              if (pdaNo.length() == 12) {
                pdaNo = pdaNo + "01";
              } else if (pdaNo.length() == 14) {
                int seq = Integer.parseInt(pdaNo.substring(12));
                pdaNo = pdaNo.substring(0, 12) + StringUtils.leftPad(Integer.toString(++seq), 2, '0');
              }
              vpdaData.setPdaNo(pdaNo);
              vpdaData.setOldId(pdaDTO.getId());
              vpdaData.setPdaStatus(PdaStatus.DFT.toString());
              vpdaData.setId(null);
              break;
          case ACC:
          case REJ:
          case CL:
            return vpdaData;
            case DFT:
            case APVD:
            case NEEDA:
            case APRPE:
            default:
                System.out.println("Default Value ");
              break;
        }


      return vpdaData;
    }

    /**
     * Used to get the list of PdaData
     *
     * @return - list of PdaData
     */
    public List<PdaDto> findAll() throws Exception {
        List<PdaData> pdaData = pdaRepository.findAllByRecordStatusTrue();

        List<PdaDto> pdaDTOS = new ArrayList<>();

        for (PdaData pdaData1 : pdaData) {
            PdaDto pdaDto = modelMapper.map(pdaData1, PdaDto.class);
            PdaProjection pdaProjection = pdaRepository.findPdaData(pdaDto.getId());

            pdaDto.setVesselName(pdaProjection.getVesselName());
            pdaDto.setBankName(pdaProjection.getBankName());
            pdaDto.setBranchName(pdaProjection.getBranchName());
            pdaDto.setCustomerAddress(pdaProjection.getCustomerAddress());
            pdaDto.setCustomerName(pdaProjection.getCustomerName());
            pdaDto.setPortName(pdaProjection.getPortName());

            if (pdaDto.getDischargePort() != null) {
                pdaDto.setDischargePortName(pdaRepository.getDischargePortName(pdaDto.getDischargePort()).getDischargePortName());
            } else {
                pdaDto.setDischargePortName("NA");
            }
            if (pdaDto.getLastPort() != null) {
                pdaDto.setLastPortName(pdaRepository.getLastPortName(pdaDto.getLastPort()).getLastPortName());
            } else {
                pdaDto.setLastPortName("NA");
            }
            if (pdaDto.getLoadPort() != null) {
                pdaDto.setLoadPortName(pdaRepository.getLoadPortName(pdaDto.getLoadPort()).getLoadPortName());
            } else {
                pdaDto.setLoadPortName("NA");
            }
            if (pdaDto.getNextPort() != null) {
                pdaDto.setNextPortName(pdaRepository.getNextPortName(pdaDto.getNextPort()).getNextPortName());
            } else {
                pdaDto.setNextPortName("NA");
            }
            if (pdaDto.getBerth() != null) {
                pdaDto.setBerthName(pdaRepository.getBerthName(pdaDto.getBerth()).getBerthName());
            } else {
                pdaDto.setBerthName("NA");
            }
            pdaDTOS.add(pdaDto);
        }

        return pdaDTOS;
    }

    /**
     * Used to get the PdaData by id
     *
     * @param id - PdaData identifier
     * @return - PdaData object
     * @throws NotFoundException - if the PdaData with the provided id is not found
     */
    public PdaDto findById(Long id) throws Exception {
        Optional<PdaData> pdaData = pdaRepository.findByIdAndRecordStatusTrue(id);

        if (!pdaData.isPresent()) {
            throw new NotFoundException(ErrorCodes.PDA_NOT_FOUND, "PDA not found");
        }
        PdaDto pdaDto = modelMapper.map(pdaData.get(), PdaDto.class);
        PdaProjection pdaProjection = pdaRepository.findPdaData(pdaDto.getId());

        pdaDto.setVesselName(pdaProjection.getVesselName());
        pdaDto.setVesselType(pdaProjection.getVesselType());
        pdaDto.setBankName(pdaProjection.getBankName());
        pdaDto.setBranchName(pdaProjection.getBranchName());
        pdaDto.setCustomerAddress(pdaProjection.getCustomerAddress());
        pdaDto.setCustomerName(pdaProjection.getCustomerName());
        pdaDto.setPortName(pdaProjection.getPortName());

        if (pdaDto.getDischargePort() != null) {
            pdaDto.setDischargePortName(pdaRepository.getDischargePortName(pdaDto.getDischargePort()).getDischargePortName());
        } else {
            pdaDto.setDischargePortName("NA");
        }
        if (pdaDto.getLastPort() != null) {
            pdaDto.setLastPortName(pdaRepository.getLastPortName(pdaDto.getLastPort()).getLastPortName());
        } else {
            pdaDto.setLastPortName("NA");
        }
        if (pdaDto.getLoadPort() != null) {
            pdaDto.setLoadPortName(pdaRepository.getLoadPortName(pdaDto.getLoadPort()).getLoadPortName());
        } else {
            pdaDto.setLoadPortName("NA");
        }
        if (pdaDto.getNextPort() != null) {
            pdaDto.setNextPortName(pdaRepository.getNextPortName(pdaDto.getNextPort()).getNextPortName());
        } else {
            pdaDto.setNextPortName("NA");
        }
        if (pdaDto.getBerth() != null) {
            pdaDto.setBerthName(pdaRepository.getBerthName(pdaDto.getBerth()).getBerthName());
        } else {
            pdaDto.setBerthName("NA");
        }

        return pdaDto;
    }

    /**
     * Used to get the company entity by id
     *
     * @param id - company identifier
     * @return - company object
     */
    private PdaData findEntityById(Long id) throws NotFoundException {
        Optional<PdaData> optionalPdaData = pdaRepository.findByIdAndRecordStatusTrue(id);
        if (!optionalPdaData.isPresent()) {
            throw new NotFoundException(ErrorCodes.PDA_NOT_FOUND, "Pda not found!");
        }
        return optionalPdaData.get();
    }

    /**
     * Used to get the PdaData by type
     *
     * @param type - PdaData identifier
     * @return - PdaData object
     * @throws NotFoundException - if the PdaData with the provided type is not found
     */
    public PdaDto findByType(String type) throws Exception {
        Optional<PdaData> pdaData = pdaRepository.findByTypeAndRecordStatusTrue(type);
        if (!pdaData.isPresent()) {
            throw new NotFoundException(ErrorCodes.PDA_NOT_FOUND, "PDA not found");
        }
        return (PdaDto) CommonUtil.copyBean(pdaData.get(), PdaDto.class);
    }

    /**
     * Used to logically delete the PdaData by updating recordStatus flag to false
     *
     * @param id - PdaData identifier
     * @return - removed PdaData identifier
     * @throws NotFoundException - if the PdaData with the provided id is not found
     */
    public Long delete(Long id) throws NotFoundException {
        PdaData pdaData = findEntityById(id);
        pdaData.setRecordStatus(false);

        PdaData deletedPda = pdaRepository.save(pdaData);
        return deletedPda.getId();
    }

  /**
   * Used to generate pdf for a pda
   *
   * @param id -pda identifier
   * @return pdf in bytes
   * @throws Exception
   */


  public byte[] generate(Long id, Constants.PdaType type, Integer group, Boolean bothCurrency) throws Exception {
    PdaReport pdaReport = getPdaData(id, type, group, bothCurrency);
    Collection<PdaReport> pdaReports = new ArrayList<PdaReport>();
    pdaReports.add(pdaReport);
    ClassPathResource res = new ClassPathResource(type != null && type.equals(Constants.PdaType.SPDA) ? "servicePda.jasper" : "pda.jasper");
    BufferedInputStream ioStream = new BufferedInputStream(res.getInputStream());
    JRBeanCollectionDataSource jrbcds = new JRBeanCollectionDataSource(pdaReports);
    final JasperReport jasperReport = (JasperReport) JRLoader.loadObject(ioStream);
    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, getJasperParameters(), jrbcds);
    return JasperExportManager.exportReportToPdf(jasperPrint);
  }

  private String formatAmount(String amount) {
    DecimalFormat formatter = new DecimalFormat("###,###,###.00");
    double formatAmount = Double.parseDouble(amount);
    return formatter.format(formatAmount);
  }

  /**
   * used to pull pda related data
   * @param id -pda identifier
   * @return PdaReport Object
   * @throws Exception
   */
  private PdaReport getPdaData(Long id, Constants.PdaType type, Integer group, Boolean bothCurrency) throws Exception {
    PdaReport pdaReport = new PdaReport();
    PdaDto pdaDTO = findById(id);
    List<Lookup> lookupList = findByKeys("COMPANY_TITLE~VESSEL_NAME_TEMPLATE");

    Map<String, String> lookupMap = lookupList.stream().collect(Collectors.toMap(Lookup::getGroupKey, Lookup::getValue));
    String vesselName = pdaDTO.getVesselName() != null ? pdaDTO.getVesselName().toUpperCase() : "TBN";

    //Title
    pdaReport.setId(pdaDTO.getId());
    pdaReport.setCompanyTitle(lookupMap.get("COMPANY_TITLE"));
      Optional<Organization> organization = organizationRepository.findByIdAndRecordStatusTrue(1L);
      if (!organization.isPresent()) {
        throw new NotFoundException(ErrorCodes.PDA_NOT_FOUND, ErrorCodes.ORGANIZATION_NOT_FOUND);
      }
    StringBuilder company = new StringBuilder();
    company.append(organization.get().getStreet())
            .append("\n")
            .append("Tel. No. + 91")
            .append(organization.get().getPhone())
            .append(" Fax No.+ 91 22 24965215 / 24910753 / 24984204")
            .append("\n")
            .append(organization.get().getEmail());
    pdaReport.setCompanyDetails(company.toString());
    pdaReport.setVesselName(type.equals(Constants.PdaType.SPDA) ? vesselName : lookupMap.get("VESSEL_NAME_TEMPLATE") + vesselName);
    pdaReport.setVesselName(lookupMap.get("VESSEL_NAME_TEMPLATE") + vesselName);
    pdaReport.setBothCurrency(bothCurrency);

    Customer customer = getCustomerData(pdaDTO.getCustomer());
    pdaReport.setCustomerName(customer.getAddress());
    pdaReport.setKindAttn(customer.getName());

    pdaReport.setPort(pdaDTO.getPortName());
    pdaReport.setOperation(pdaDTO.getOperation());
    pdaReport.setCargo(pdaDTO.getCargo());
    pdaReport.setGrt(pdaDTO.getGrt());
    pdaReport.setNrt(pdaDTO.getNrt());
    pdaReport.setRgt(pdaDTO.getRgt());
    pdaReport.setRoe(pdaDTO.getRoe());
    pdaReport.setDischargeCargo(pdaDTO.getDischargeCargo());
    pdaReport.setDischargeQty(pdaDTO.getDischargeQty());
    pdaReport.setLoadCargo(pdaDTO.getLoadCargo());
    pdaReport.setLoadQty(pdaDTO.getQuantity());
    pdaReport.setBerth(pdaRepository.getBerthName(pdaDTO.getBerth()).getBerthName());

    String eta = pdaDTO.getEta().replace(" 00:00:00", "");
    pdaReport.setEtaDate(eta);

    pdaReport.setCurrency("Total Amount \n (" + pdaDTO.getOrgCurrency() + ")");

    //PdaContent
    Map<String, Object> itemMap = getPdaItems(id, pdaDTO.getOrgCurrency(), type, group, bothCurrency, pdaDTO.getRoe());
    pdaReport.setPdaItems((List<PdaItem>) itemMap.get("PdaItems"));
    pdaReport.setTotal(formatAmount((String) itemMap.get("TotalAmount")));
    pdaReport.setTotalInr(formatAmount((String) itemMap.get("TotalAmountInr")));

    //Bank Details
    BankDTO bankDTO = getBankDetails(pdaDTO.getBank());
    pdaReport.setBeneficiary(bankDTO.getBeneficiaryName());
    pdaReport.setAccountNo(bankDTO.getBankAccountNo());
    pdaReport.setBankName(bankDTO.getBank() + "\n" + bankDTO.getBankAddress());
    pdaReport.setSwiftCode(bankDTO.getSwiftCode());
    pdaReport.setThrough("A/C. NO. 544774311");
    pdaReport.setOf("BANK OF INDIA\nNEW YORK, U.S.A.\nBKIDUS33");
    pdaReport.setIfscCode(bankDTO.getIfscCode());
    pdaReport.setAbaNo("026005458");
    pdaReport.setPurpose("SHIP DISBURSEMENT \nM/T.  TBN AT PORT SIKKA");

    //Note
    pdaReport.setNote(getNotes(pdaDTO.getPort(), pdaDTO.getCustomer()));

    //Information
    //pdaReport.setInfo("As per world scale  below mention charges are on charterers A/C. \nIf RIL India is charterers they settle these charges direcly with Sikka port terminal." +
    //        "\nHowever if charterer is other than RIL than below mention Charges also to be paid by owners \nto agent and owners may settle directly with Charterer as per their C/P.-World Scale.");
    pdaReport.setInfo(null);

    //Additional Info
    /*pdaReport.setAdditionalInformationList(getAdditionalInfo(id));*/
    pdaReport.setAdditionalInformationList(null);
    return pdaReport;
  }

  private List<Lookup> findByKeys(String keys) {
        String[] keyList = keys.split(Constants.LOOKUP_KEY_SPLITTER);
        if (keyList.length == 0) {
            return Collections.emptyList();
        }

    return lookupRepository.findByGroupKeyIn(keyList);
    }

  /**
   * used to pull note template for pda based on port
   * @param portId -port Identifier
   * @param customerId -customer identifier
   * @return notes in string
   */
  private String getNotes(Long portId, Long customerId) {
    StringBuilder notes = new StringBuilder();
    Map<String, List<Template>> templateMap = new HashMap<>();
    List<Template> templateList = templateRepository.findAllByKeyPortIdCustomerId(portId, customerId);
    for (Template template : templateList) {
      if (templateMap.containsKey(template.getGroupKey())) {
        templateMap.get(template.getGroupKey()).add(template);
      } else {
        List<Template> templates = new ArrayList<>();
        templates.add(template);
        templateMap.put(template.getGroupKey(), templates);
      }
    }
    int srNo = 0;
    for (String key : templateMap.keySet()) {
      List<Template> noteList = templateMap.get(key);
      for (Template template : noteList) {
        notes.append(++srNo)
                .append(")")
                .append(template.getValue())
                .append("\n");
      }
    }

    return notes.toString();
  }

  private Customer getCustomerData(Long id) throws Exception {
        Optional<Customer> customerData = customerRepository.findByIdAndRecordStatusTrue(id);
        if (!customerData.isPresent()) {
            throw new NotFoundException(ErrorCodes.PDA_NOT_FOUND, ErrorCodes.CUSTOMER_NOT_FOUND);
        }
        return customerData.get();
    }

    private BankDTO getBankDetails(Long id) throws Exception {
        Optional<BankDetails> bankData = bankRepository.findByIdAndRecordStatusTrue(id);
        if (!bankData.isPresent()) {
            throw new NotFoundException(ErrorCodes.PDA_NOT_FOUND, Constants.BANK_NOT_FOUND);
        }
        return (BankDTO) CommonUtil.copyBean(bankData.get(), BankDTO.class);
    }

    private List<AdditionalInformation> getAdditionalInfo(Long id) {
        List<AdditionalInformation> additionalInformations = new ArrayList<>();

        AdditionalInformation additionalInformation = new AdditionalInformation();
        additionalInformation.setParticulars("Port Tonnage Dues");
        additionalInformation.setBreakUp("Usd 0.09 x  per 271000 mt (approx)");
        additionalInformation.setAmount("25,000.00");
        additionalInformations.add(additionalInformation);

        additionalInformation = new AdditionalInformation();
        additionalInformation.setParticulars("Port Tonnage Dues");
        additionalInformation.setBreakUp("Usd 0.09 x  per 271000 mt (approx)");
        additionalInformation.setAmount("25,000.00");
        additionalInformations.add(additionalInformation);

        additionalInformation = new AdditionalInformation();
        additionalInformation.setParticulars("Port Tonnage Dues");
        additionalInformation.setBreakUp("Usd 0.09 x  per 271000 mt (approx)");
        additionalInformation.setAmount("25,000.00");
        additionalInformations.add(additionalInformation);

        additionalInformation = new AdditionalInformation();
        additionalInformation.setParticulars("Port Tonnage Dues");
        additionalInformation.setBreakUp("Usd 0.09 x  per 271000 mt (approx)");
        additionalInformation.setAmount("25,000.00");
        additionalInformations.add(additionalInformation);

        additionalInformation = new AdditionalInformation();
        additionalInformation.setParticulars("<b>Total</b>");
        additionalInformation.setBreakUp(null);
        additionalInformation.setAmount("<b>25,000.00</b>");
        additionalInformations.add(additionalInformation);

        return additionalInformations;
    }

  /**
   * used to pull pda Services in pda
   * @param id -pda identifier
   * @param orgCurrency -Organization currency
   * @param roe
   * @return map
   * @throws Exception
   */
  private Map<String, Object> getPdaItems(Long id, String orgCurrency, Constants.PdaType type, Integer group, Boolean bothCurrency, String roe) {
    List<PdaServicesDto> pdaServices = pdaServicesService.findByPdaId(id, group, type);
    if (pdaServices.isEmpty()) {
      throw new UnprocessableException(ErrorCodes.PDA_SERVICE_NOT_FOUND, "PDA Services Not Found");
    }
    Map<String, Object> pdaItemMap = new HashMap<>();
    List<PdaItem> pdaItems = new ArrayList<>();
    BigDecimal rateOfExchange = new BigDecimal(0) ;
    if(bothCurrency){
      if ((roe == null || roe.isEmpty() || roe.equalsIgnoreCase("0"))) {
        throw new UnprocessableException(ErrorCodes.ROE_REQUIRED, ErrorCodes.ROE_REQUIRED);
      }
      else{
        rateOfExchange = new BigDecimal(roe);
      }
    }

    Map<String, List<PdaServicesDto>> serviceMap = getPdaServicesByChargeCategory(pdaServices);
    BigDecimal totalAmount = new BigDecimal(0);
    BigDecimal totalAmountInr = new BigDecimal(0);
        int srNo = 0;

    for (String key : serviceMap.keySet()) {


      List<PdaServicesDto> pdaServicesDtos = serviceMap.get(key);

      pdaServicesDtos.sort(new PdaServiceSortingComparator());

      PdaItem pdaItem = new PdaItem();
      pdaItem.setParticulars("<b>" + key + "</b>");
      pdaItem.setBothCurrency(bothCurrency);
      pdaItems.add(pdaItem);

      Long taxRate = new Long(pdaServicesDtos.get(0).getTaxRate());
      BigDecimal taxAmount = new BigDecimal(0);
      BigDecimal taxAmountInr = new BigDecimal(0);

      for (PdaServicesDto pdaServicesDto : pdaServicesDtos) {

        pdaItem = new PdaItem();
        pdaItem.setParticulars(pdaServicesDto.getDescription());
        pdaItem.setBreakUp(getBreakup(pdaServicesDto, orgCurrency));
        BigDecimal amountWithTax = new BigDecimal(pdaServicesDto.getTotalAmount());
        totalAmount = totalAmount.add(amountWithTax);
        pdaItem.setAmount(formatAmount(pdaServicesDto.getAmount()));
        pdaItem.setBothCurrency(bothCurrency);
        BigDecimal amountInr = new BigDecimal(pdaServicesDto.getAmount()).multiply(rateOfExchange);
        pdaItem.setAmountInr(formatAmount(amountInr.toString()));
        totalAmountInr = totalAmountInr.add(amountWithTax.multiply(rateOfExchange));
        Long serviceTaxRate = new Long(pdaServicesDto.getTaxRate());
        if (taxRate.longValue() == serviceTaxRate.longValue() && taxRate != 0) {
          BigDecimal serviceTaxAmount = new BigDecimal(pdaServicesDto.getTaxAmount());
          taxAmount = taxAmount.add(serviceTaxAmount);
          taxAmountInr = taxAmountInr.add(serviceTaxAmount.multiply(rateOfExchange));
        } else if (taxRate != 0) {
          PdaItem pdaItemTax = new PdaItem();
          pdaItemTax.setSerialNumber(++srNo);
          pdaItemTax.setParticulars(taxRate + "% GST");
          pdaItemTax.setBreakUp(null);
          pdaItemTax.setAmount(formatAmount(taxAmount.toString()));
          pdaItemTax.setAmountInr(formatAmount(taxAmountInr.toString()));
          taxAmount = new BigDecimal(0);
          taxAmountInr = new BigDecimal(0);
          BigDecimal serviceTaxAmount = new BigDecimal(pdaServicesDto.getTaxAmount());
          taxAmount = taxAmount.add(serviceTaxAmount);
          totalAmountInr = totalAmountInr.add(amountWithTax.multiply(rateOfExchange));
          taxRate = serviceTaxRate;
          pdaItems.add(pdaItemTax);
        }
        pdaItem.setSerialNumber(++srNo);
        pdaItems.add(pdaItem);

      }
      if (taxRate != 0) {
        PdaItem pdaItemTax = new PdaItem();
        pdaItemTax.setSerialNumber(++srNo);
        pdaItemTax.setParticulars(taxRate + "% GST");
        pdaItemTax.setBreakUp(null);
        pdaItemTax.setAmount(formatAmount(taxAmount.toString()));
        pdaItemTax.setAmountInr(formatAmount(taxAmountInr.toString()));
        pdaItems.add(pdaItemTax);
      }
    }

    pdaItemMap.put("PdaItems", pdaItems);
    pdaItemMap.put("TotalAmount", totalAmount.toString());
    pdaItemMap.put("TotalAmountInr", totalAmountInr.toString());
    return pdaItemMap;
  }

  /**
   * used to re-arrange pda service based on charge category
   * @param pdaServices -list of pda services
   * @return map
   */
  private Map<String, List<PdaServicesDto>> getPdaServicesByChargeCategory(List<PdaServicesDto> pdaServices) {
    Map<String, List<PdaServicesDto>> serviceMap = new HashMap<>();

    for (PdaServicesDto pdaServicesDTO : pdaServices) {

      if (serviceMap.containsKey(pdaServicesDTO.getChargeCategory())) {
        serviceMap.get(pdaServicesDTO.getChargeCategory()).add(pdaServicesDTO);
      } else {
        List<PdaServicesDto> pdaServicesDtos = new ArrayList<>();
        pdaServicesDtos.add(pdaServicesDTO);
        serviceMap.put(pdaServicesDTO.getChargeCategory(), pdaServicesDtos);

      }
    }
    return serviceMap;
  }

  /**
   * used to construct break up for a particular service
   * @param pdaServicesDto -domain object
   * @param orgCurrency -organization currency
   * @return breakup as string(5 X GRT)
   */
  private String getBreakup(PdaServicesDto pdaServicesDto, String orgCurrency) {

    StringBuilder breakup = new StringBuilder();
    breakup.append(pdaServicesDto.getCurrency().equals(orgCurrency) ? "" : pdaServicesDto.getCurrency())
            .append(pdaServicesDto.getCurrency().equals(orgCurrency) ? "" : " ");
    switch (pdaServicesDto.getUnitOfMeasure()) {
      case "GRT":
      case "NRT":
      case "RGRT":
      case "DAY":
      case "HOUR":
        breakup.append(pdaServicesDto.getRate())
                .append(" x ")
                .append(pdaServicesDto.getUnitOfMeasure());
        break;
      case "FIXED":
        breakup.append("Fixed");
        break;
      default:
        breakup.append(pdaServicesDto.getRate())
                .append(" x ")
                .append(pdaServicesDto.getUnitOfMeasure().split("~")[0])
                .append(" x ")
                .append(pdaServicesDto.getTime())
                .append(" ")
                .append(pdaServicesDto.getUnitOfMeasure().split("~")[1])
                .append(pdaServicesDto.getTime() > 1 ? "S" : "");
    }
    return breakup.toString();
  }

    private Map getJasperParameters() {
        Map<String, String> jasperParams = new HashMap<>();
        jasperParams.put("SUBREPORT_DIR", Constants.REPORT_BASE_PATH);
        return jasperParams;
    }

  public String getFileName(Long id, Constants.PdaType type) throws Exception {
      PdaDto pdaDto = findById(id);
      StringBuilder fileName = new StringBuilder();
        fileName.append(pdaDto.getPdaNo())
                .append("_")
                .append(pdaDto.getVesselName() != null ? pdaDto.getVesselName() : "TBN")
                .append("_")
                .append(pdaDto.getPortName() != null ? pdaDto.getPortName() : "")
                .append(pdaDto.getPortName() != null ? "_" : "")
                .append(pdaDto.getCargo() != null ? pdaDto.getCargo() : "")
                .append(pdaDto.getCargo() != null ? "_" : "")
                .append(pdaDto.getOperation() != null ? pdaDto.getOperation() : "");
    if (type.equals(Constants.PdaType.SPDA)) {
      fileName.append(pdaDto.getOperation() != null ? "_" : "")
              .append("S");
    }
        return fileName.toString();
    }

    /**
     * Used to get pdaOnLoadDTO
     *
     * @param token - OrgId identifier
     * @return -  PdaOnLoadDTO identifier
     */
    public PdaOnLoadDto getPdaOnLoadDTO(String token) throws UnauthorizedException {
      Token tokenData = tokenService.findEntityByToken(token);
        PdaOnLoadDto pdaOnLoadDTO = new PdaOnLoadDto();
        //set data to DTO
        pdaOnLoadDTO.setPdaType(getLookupData(tokenData.getOrgId(), "PDA_TYPE"));
        pdaOnLoadDTO.setCurrency(getLookupData(tokenData.getOrgId(), "CURRENCY"));
        pdaOnLoadDTO.setCargo(getLookupData(tokenData.getOrgId(), "CARGO"));
        pdaOnLoadDTO.setLoadCargo(getLookupData(tokenData.getOrgId(), "LOAD_CARGO"));
        pdaOnLoadDTO.setDischargeCargo(getLookupData(tokenData.getOrgId(), "DISCHARGE_CARGO"));
        pdaOnLoadDTO.setDurationUom(getLookupData(tokenData.getOrgId(), "DURATION_UOM"));
        pdaOnLoadDTO.setOperation(getLookupData(tokenData.getOrgId(), "OPERATION"));
        pdaOnLoadDTO.setPdaStatus(getLookupData(tokenData.getOrgId(), "PDA_STATUS"));
        pdaOnLoadDTO.setBank(getBankData(tokenData.getOrgId()));
        pdaOnLoadDTO.setVesselTrade(getVesselTrade(tokenData.getOrgId(), "TRADE"));

        return pdaOnLoadDTO;
    }


  /**
     * Used to get vesselTrade
     *
     * @param orgId - OrgId identifier
     * @return -  map of vesselTrade
     */
    private Map<String, String> getVesselTrade(Long orgId, String groupKey) {

        Map<String, String> map = new HashMap<>();
        List<Lookup> lookupList = lookupRepository.findByGroupKeyAndStatusAndRecordStatusTrue(groupKey, Status.ACTIVE);

        for (Lookup l : lookupList) {
            map.put(l.getCode(), l.getValue());
        }
        return map;
    }


    /**
     * Used to get BankData
     *
     * @param orgId - OrgId identifier
     * @return -  map of BankData
     */
    private Map<String, String> getBankData(Long orgId) {

        Map<String, String> map = new HashMap<>();

      List<BankDetails> bankDataList;
        if (orgId != 0) {
            bankDataList = bankRepository.findByOrgIdAndRecordStatusTrue(orgId);
        } else {
            bankDataList = bankRepository.findByRecordStatusTrue();
        }
        for (BankDetails bankDetails : bankDataList) {
            map.put(bankDetails.getId().toString(), bankDetails.getBank());
        }
        return map;
    }

    /**
     * Used to get lookupData
     *
     * @param orgId,groupKey - OrgId identifier
     * @return -  map of lookupData
     */
    private Map<String, String> getLookupData(Long orgId, String groupKey) {
      List<Lookup> lookupList;
        Map<String, String> map = new HashMap<>();
        if (orgId != 0) {
            lookupList = lookupRepository.findByGroupKeyAndOrgIdAndStatusAndRecordStatusTrue(groupKey, orgId, Status.ACTIVE);
        } else {
            lookupList = lookupRepository.findByGroupKeyAndStatusAndRecordStatusTrue(groupKey, Status.ACTIVE);
        }
        for (Lookup l : lookupList) {
            map.put(l.getCode(), l.getValue());
        }
        return map;
    }

  /**
   * Used to get pda status and update pda
   *
   * @param id,status,remark - identifier
   */
  public void updatePdaStatus(Long id, String status, String remark) throws Exception {
    Optional<PdaData> pdaDataOptional = pdaRepository.findByIdAndRecordStatusTrue(id);
    if (pdaDataOptional.isPresent()) {
      PdaStatus pdaStatus = PdaStatus.valueOf(status);
      List<PdaServices> pdaServices = pdaServicesRepository.findAllByPdaIdAndRecordStatusTrue(id);
      switch (pdaStatus) {
        case SUBM:
        case ACC:
        case APRPE:
        case APVD:
          if (pdaServices.isEmpty()) {
            throw new NotFoundException(ErrorCodes.SERVICE_NOT_FOUND, "Services not found!");
          }
          break;
        default:
          break;
      }
      pdaDataOptional.get().setPdaStatus(status);
      if (remark != null && !remark.isEmpty()) {
        pdaDataOptional.get().setRemark(remark);
      }
      pdaRepository.save(pdaDataOptional.get());
    } else {
      throw new NotFoundException(ErrorCodes.PDA_NOT_FOUND, "Pda not found!");
    }
  }

  public List<PdaDto> getAcceptedJobNo() {
    List<PdaData> pdaDataList = pdaRepository.findByPdaStatusAndRecordStatusTrue(PdaStatus.ACC.name());
    List<PdaDto> pdaDtoList = new ArrayList<>();
    if (pdaDataList.isEmpty()) {
      return pdaDtoList;
    }
    for (PdaData pdaData : pdaDataList) {
      PdaDto pdaDto = modelMapper.map(pdaData, PdaDto.class);
      PdaProjection pdaProjection = pdaRepository.findPdaData(pdaDto.getId());
      pdaDto.setVesselName(pdaProjection.getVesselName());
      pdaDto.setBankName(pdaProjection.getBankName());
      pdaDto.setBranchName(pdaProjection.getBranchName());
      pdaDto.setCustomerAddress(pdaProjection.getCustomerAddress());
      pdaDto.setCustomerName(pdaProjection.getCustomerName());
      pdaDto.setPortName(pdaProjection.getPortName());
      if (pdaDto.getDischargePort() != null) {
        pdaDto.setDischargePortName(pdaRepository.getDischargePortName(pdaDto.getDischargePort()).getDischargePortName());
      } else {
        pdaDto.setDischargePortName("NA");
      }
      if (pdaDto.getLastPort() != null) {
        pdaDto.setLastPortName(pdaRepository.getLastPortName(pdaDto.getLastPort()).getLastPortName());
      } else {
        pdaDto.setLastPortName("NA");
      }
      if (pdaDto.getLoadPort() != null) {
        pdaDto.setLoadPortName(pdaRepository.getLoadPortName(pdaDto.getLoadPort()).getLoadPortName());
      } else {
        pdaDto.setLoadPortName("NA");
      }
      if (pdaDto.getNextPort() != null) {
        pdaDto.setNextPortName(pdaRepository.getNextPortName(pdaDto.getNextPort()).getNextPortName());
      } else {
        pdaDto.setNextPortName("NA");
      }
      if (pdaDto.getBerth() != null) {
        pdaDto.setBerthName(pdaRepository.getBerthName(pdaDto.getBerth()).getBerthName());
      } else {
        pdaDto.setBerthName("NA");
      }
      pdaDtoList.add(pdaDto);
    }
    return pdaDtoList;
  }
}
