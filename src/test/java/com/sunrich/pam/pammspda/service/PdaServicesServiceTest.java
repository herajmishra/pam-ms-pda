package com.sunrich.pam.pammspda.service;

import com.sunrich.pam.common.domain.pda.PdaServices;
import com.sunrich.pam.common.dto.pda.PdaServicesDto;
import com.sunrich.pam.common.exception.BaseRuntimeException;
import com.sunrich.pam.pammspda.repository.PdaServicesRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class PdaServicesServiceTest {
    @Mock
    PdaServicesRepository pdaServicesRepository;

    @InjectMocks
    PdaServicesService pdaServicesService;

    private List<PdaServices> pdaServicesList;
    private List<PdaServicesDto> pdaServicesDtoList;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @Before
    public void setUp() {
        pdaServicesList = Arrays.asList(
                PdaServices.builder().id(2L).tariffId(2L).recordStatus(true).build(),
                PdaServices.builder().id(3L).tariffId(3L).recordStatus(true).build()
        );

        pdaServicesDtoList = Arrays.asList(
                PdaServicesDto.builder().id(2L).tariffId(2L).build(),
                PdaServicesDto.builder().id(3L).tariffId(3L).build()
        );

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    }

    @Test
    public void findAll_whenRecordsExists_shouldReturnRecords() {
        when(pdaServicesRepository.findAllByRecordStatusTrueOrderByIdDesc()).thenReturn(pdaServicesList);
        assertThat(pdaServicesService.findAll()).isEqualTo(pdaServicesDtoList);
        verify(pdaServicesRepository).findAllByRecordStatusTrueOrderByIdDesc();
    }

    @Test
    public void findById_whenRecordWithGivenIdExists_shouldReturnCorrespondingObject() {
        when(pdaServicesRepository.findByIdAndRecordStatusTrue(2L)).thenReturn(Optional.ofNullable(pdaServicesList.get(0)));
        assertThat(pdaServicesService.findById(2L)).isEqualTo(pdaServicesDtoList.get(0));
        verify(pdaServicesRepository).findByIdAndRecordStatusTrue(2L);
    }

    @Test(expected = BaseRuntimeException.class)
    public void findById_whenRecordWithGivenIdDoesNotExist_shouldThrowNotFoundException() {
        pdaServicesService.findById(5L);
        verify(pdaServicesRepository).findByIdAndRecordStatusTrue(5L);
    }

    @Test
    public void save_whenValidObject_shouldSaveAndReturnSavedObject() {
        PdaServices.PdaServicesBuilder builder = PdaServices.builder().id(null).tariffId(2L).recordStatus(false);

        PdaServices vesselToSave = builder.recordStatus(true).build(); // input for repo layer save method
        PdaServices savedVessel = builder.id(2L).recordStatus(true).build(); // result of repo layer save method

        PdaServicesDto newDto = PdaServicesDto.builder().tariffId(2L).build();
        PdaServicesDto savedDto = PdaServicesDto.builder().id(2L).tariffId(2L).build();

        when(pdaServicesRepository.save(vesselToSave)).thenReturn(savedVessel);
      assertThat(pdaServicesService.saveOrUpdate(newDto, "DRAFT")).isEqualTo(savedDto);
        verify(pdaServicesRepository).save(vesselToSave);
    }

    @Test
    public void update_whenValidObject_shouldSaveAndReturnSavedObject() {
        long id = 1L;
        long updatedTariffId = 2L;

        PdaServices.PdaServicesBuilder builder = PdaServices.builder().id(id).tariffId(1L);

        PdaServicesDto payload = PdaServicesDto.builder().id(1L).tariffId(updatedTariffId).build();
        PdaServices findByIdResult = builder.recordStatus(true).build();
        PdaServices pdaServicesToUpdate = builder.tariffId(updatedTariffId).recordStatus(true).build();

        when(pdaServicesRepository.findByIdAndRecordStatusTrue(id)).thenReturn(Optional.ofNullable(findByIdResult));
        when(pdaServicesRepository.save(pdaServicesToUpdate)).thenReturn(pdaServicesToUpdate);

      assertThat(pdaServicesService.saveOrUpdate(payload, "DRAFT")).isEqualTo(payload);

        verify(pdaServicesRepository).findByIdAndRecordStatusTrue(id);
        verify(pdaServicesRepository).save(pdaServicesToUpdate);
    }

    @Test(expected = BaseRuntimeException.class)
    public void update_whenNonExistingObject_shouldThrowNotFoundException() {
        long id = 1L;
        PdaServicesDto pdaServicesDTO = PdaServicesDto.builder().id(id).build();

        when(pdaServicesRepository.findByIdAndRecordStatusTrue(id)).thenReturn(Optional.empty());
        try {
          pdaServicesService.saveOrUpdate(pdaServicesDTO, "DRAFT");
        } finally {
            verify(pdaServicesRepository).findByIdAndRecordStatusTrue(id);
        }
    }

    @Test
    public void delete_whenRecordWithGivenIdExists_shouldUpdateRecordStatusToFalseAndReturnTheId() {
        Long[] id = {1L, 2L};

        PdaServices.PdaServicesBuilder builder = PdaServices.builder().id(id[0]).tariffId(1L);

        PdaServices pdaServices = builder.recordStatus(true).build();
        PdaServices pdaServicesToDelete = builder.recordStatus(false).build();

        when(pdaServicesRepository.findByIdAndRecordStatusTrue(id[0])).thenReturn(Optional.ofNullable(pdaServices));
        when(pdaServicesRepository.save(pdaServicesToDelete)).then(returnsFirstArg());

        pdaServicesService.delete(id);

        verify(pdaServicesRepository).findByIdAndRecordStatusTrue(id[0]);
        verify(pdaServicesRepository).save(pdaServicesToDelete);
    }
}
