package com.sunrich.pam.pammspda.service;


import com.sunrich.pam.common.domain.pda.PdaData;
import com.sunrich.pam.common.dto.pda.PdaDto;
import com.sunrich.pam.common.enums.PdaStatus;
import com.sunrich.pam.common.exception.NotFoundException;
import com.sunrich.pam.pammspda.repository.PdaRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class PdaServiceTest {

    @Mock
    PdaRepository pdaRepository;

    @InjectMocks
    PdaService pdaService;

    PdaData pdaData;
    PdaData pdaDataStatusTrue;
    PdaData pdaDataBeforeMck;
    PdaData pdaDataAfterMck;
    PdaData pdaDataForUpdate;
    PdaData pdaDataToUpdate;
    PdaData existingPdaData;
    PdaData deletedPdaData;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        long id = 1L;

        pdaDataStatusTrue = PdaData.builder().id(1L).type("AA").recordStatus(true).build();
        pdaData = PdaData.builder().type("AOP").recordStatus(false).build(); //input for service save method
        pdaDataBeforeMck = PdaData.builder().type("AOP").recordStatus(true).build(); //input for repository save method
        pdaDataAfterMck = PdaData.builder().id(1L).type("AOP").recordStatus(true).build(); //result of repository save method
        pdaDataForUpdate = PdaData.builder().id(id).recordStatus(false).build(); // input for service layer save method
        pdaDataToUpdate = PdaData.builder().id(id).recordStatus(true).build(); // input and result for repo layer save method
        existingPdaData = PdaData.builder().id(id).recordStatus(true).type("BB").build(); // existing berth mock
        deletedPdaData = PdaData.builder().id(1L).type("AA").recordStatus(false).build();
    }

    @Test
    public void save_whenValidObject_shouldSaveAndReturnSavedObject() throws Exception {

        PdaData.PdaDataBuilder builder = PdaData.builder().id(null).type("AA").recordStatus(false);

        PdaData pdaDataToSave = builder.recordStatus(true).build(); // input for repo layer save method
        PdaData savedPdaData = builder.id(1L).recordStatus(true).build(); // result of repo layer save method

        PdaDto newDto = PdaDto.builder().type("AA").build();
        PdaDto savedDto = PdaDto.builder().id(1L).type("AA").build();

        when(pdaRepository.save(pdaDataToSave)).thenReturn(savedPdaData);
        assertThat(pdaService.saveOrUpdate(newDto)).isEqualTo(savedDto);
        verify(pdaRepository).save(pdaDataToSave);

    }

    @Test
    public void update_whenValidObject_shouldSaveAndReturnSavedObject() throws Exception {
        long id = 1L;
        PdaData.PdaDataBuilder builder = PdaData.builder().id(id).type("AA").pdaStatus(PdaStatus.DFT.toString());

        PdaDto payload = PdaDto.builder().id(1L).type("AA").pdaStatus(PdaStatus.DFT.toString()).build();
        PdaData findByIdResult = builder.recordStatus(true).build();
        PdaDto saveInput = PdaDto.builder().id(1L).type("AA").pdaStatus(PdaStatus.DFT.toString()).build();
        PdaData saveInputData = PdaData.builder().id(1L).type("AA").recordStatus(true).pdaStatus(PdaStatus.DFT.toString()).build();

        when(pdaRepository.findByIdAndRecordStatusTrue(id)).thenReturn(Optional.ofNullable(findByIdResult));
        when(pdaRepository.save(findByIdResult)).thenReturn(saveInputData);

        assertThat(pdaService.saveOrUpdate(payload)).isEqualTo(saveInput);

        verify(pdaRepository).findByIdAndRecordStatusTrue(id);
        verify(pdaRepository).save(saveInputData);
    }

    @Test(expected = NotFoundException.class)
    public void update_whenNonExistingObject_shouldThrowNotFoundException() throws Exception {
        long id = 1L;
        PdaDto pdaDTO = PdaDto.builder().id(id).build();

        when(pdaRepository.findByIdAndRecordStatusTrue(id)).thenReturn(Optional.empty());
        try {
            pdaService.saveOrUpdate(pdaDTO);
        } finally {
            verify(pdaRepository).findByIdAndRecordStatusTrue(id);
        }
    }

    @Test
    public void findAll_whenRecordsExists_shouldReturnRecords() throws Exception{

        List<PdaData> pdaDataList = Arrays.asList(
                PdaData.builder().id(1L).type("AA").recordStatus(true).build(),
                PdaData.builder().id(2L).type("BB").recordStatus(true).build(),
                PdaData.builder().id(3L).type("CC").recordStatus(true).build()
        );

        List<PdaDto> pdaDtoList = Arrays.asList(
                PdaDto.builder().id(1L).type("AA").build(),
                PdaDto.builder().id(2L).type("BB").build(),
                PdaDto.builder().id(3L).type("CC").build()
        );

        when(pdaRepository.findAllByRecordStatusTrue()).thenReturn(pdaDataList);
        assertThat(pdaService.findAll()).isEqualTo(pdaDtoList);
        verify(pdaRepository).findAllByRecordStatusTrue();
    }

    @Test
    public void findById_whenRecordWithGivenIdDoesNotExist_shouldThrowNotFoundException() throws Exception {

        when(pdaRepository.findByIdAndRecordStatusTrue(1L)).thenReturn(Optional.ofNullable(pdaDataStatusTrue));

        Long id = 1L;
        PdaDto result = pdaService.findById(id);
        assertThat(id).isEqualTo(result.getId());

    }

    @Test
    public void findByName_whenRecordWithGivenNameExists_shouldReturnCorrespondingObject() throws Exception {

        when(pdaRepository.findByTypeAndRecordStatusTrue("AA")).thenReturn(Optional.ofNullable(pdaDataStatusTrue));

        String type = "AA";
        PdaDto result = pdaService.findByType(type);
        assertThat("AA").isEqualTo(result.getType());
    }

    @Test
    public void delete_whenRecordWithGivenIdExists_shouldUpdateRecordStatusToFalseAndReturnTheId() throws NotFoundException {
        Long id = 1L;

        when(pdaRepository.findByIdAndRecordStatusTrue(id)).thenReturn(Optional.ofNullable(pdaDataStatusTrue));
        when(pdaRepository.save(pdaDataStatusTrue)).thenReturn(deletedPdaData);

        assertThat(id).isEqualTo(pdaService.delete(deletedPdaData.getId()));

        verify(pdaRepository, times(1)).findByIdAndRecordStatusTrue(id);
        verify(pdaRepository, times(1)).save(pdaDataStatusTrue);

    }
}