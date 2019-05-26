package com.sunrich.pam.pammspda.web.controller;

import com.sunrich.pam.common.constants.Constants;
import com.sunrich.pam.common.dto.pda.PdaServicesDto;
import com.sunrich.pam.pammspda.service.PdaServicesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@RequestMapping(path = "/pdaService")
public class PdaServicesController {

    private PdaServicesService pdaServicesService;

    public PdaServicesController(PdaServicesService pdaServicesService) {
        this.pdaServicesService = pdaServicesService;
    }

    @PostMapping
    public ResponseEntity save(@RequestBody @Valid List<PdaServicesDto> payload, @RequestParam String status) {
        List<PdaServicesDto> listOfPdaServicesDTO = new ArrayList<>();
        for (PdaServicesDto pdaService : payload) {

            PdaServicesDto pdaServicesDTO = pdaServicesService.saveOrUpdate(pdaService, status);
            listOfPdaServicesDTO.add(pdaServicesDTO);
        }
        return ResponseEntity.ok().body(listOfPdaServicesDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity findById(@PathVariable Long id) {
        PdaServicesDto pdaServicesDTO = pdaServicesService.findById(id);
        return ResponseEntity.ok().body(pdaServicesDTO);
    }

    @GetMapping("/pdaId/{id}")
    public ResponseEntity findByPdaId(@PathVariable Long id, @RequestParam(required = false) Integer group, @RequestParam Constants.PdaType type) {
        List<PdaServicesDto> pdaServicesDtoList = pdaServicesService.findByPdaId(id, group, type);
        return ResponseEntity.ok().body(pdaServicesDtoList);
    }

    @GetMapping
    public ResponseEntity findAll() {
        List<PdaServicesDto> pdaServicesDTOS = pdaServicesService.findAll();
        return ResponseEntity.ok().body(pdaServicesDTOS);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long[] id) {
        pdaServicesService.delete(id);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @GetMapping("/findAllServicesForBranchRequisition/{pdaId}")
    public ResponseEntity findAllPdaServicesAndServicePdaServices(@PathVariable("pdaId") Long pdaId) {
        List<PdaServicesDto> pdaServicesDTOS = pdaServicesService.findAllPdaServicesAndServicePdaServices(pdaId);
        return ResponseEntity.ok().body(pdaServicesDTOS);
    }
}
