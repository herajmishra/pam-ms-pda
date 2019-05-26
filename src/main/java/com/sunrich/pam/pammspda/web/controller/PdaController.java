package com.sunrich.pam.pammspda.web.controller;

import com.sunrich.pam.common.constants.Constants;
import com.sunrich.pam.common.dto.pda.PdaDto;
import com.sunrich.pam.common.dto.pda.PdaOnLoadDto;
import com.sunrich.pam.common.exception.NotFoundException;
import com.sunrich.pam.common.exception.UnauthorizedException;
import com.sunrich.pam.pammspda.service.EmailService;
import com.sunrich.pam.pammspda.service.PdaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Exposes crud operation endpoints for PDA
 */
@RestController
@Slf4j
@RequestMapping(path = "/")
public class PdaController {

    @Autowired
    PdaService pdaService;

    @Autowired
    EmailService emailService;

  @GetMapping("/onLoad")
  public ResponseEntity pdaOnLoad(@RequestHeader("token") String token) throws UnauthorizedException {
        PdaOnLoadDto data = pdaService.getPdaOnLoadDTO(token);
        return ResponseEntity.ok().body(data);
    }

    @PostMapping
    public ResponseEntity save(@RequestBody @Valid PdaDto payload) throws Exception {
        PdaDto pdaDTO = pdaService.saveOrUpdate(payload);
        return ResponseEntity.ok().body(pdaDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity findById(@PathVariable Long id) throws Exception {
        PdaDto pdaDTO = pdaService.findById(id);
        return ResponseEntity.ok().body(pdaDTO);
    }

    @GetMapping("/findByType/{type}")
    public ResponseEntity findByType(@PathVariable String type) throws Exception {
        PdaDto pdaDTO = pdaService.findByType(type);
        return ResponseEntity.ok().body(pdaDTO);
    }

    @GetMapping
    public ResponseEntity findAll() throws Exception {
        List<PdaDto> pdaDtos = pdaService.findAll();
        return ResponseEntity.ok().body(pdaDtos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable Long id) throws NotFoundException {
        Long pdaId = pdaService.delete(id);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @GetMapping("/generate/{id}")
    public ResponseEntity<byte[]> generate(@PathVariable Long id, @RequestParam(required = false) Integer group, @RequestParam Constants.PdaType type, @RequestParam Boolean bothCurrency) throws Exception {
      String fileName = pdaService.getFileName(id, type);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_PDF);
        httpHeaders.add("Content-Disposition", "filename=\"" + fileName + ".pdf\"");
      return ResponseEntity.ok().headers(httpHeaders).body(pdaService.generate(id, type, group, bothCurrency));
    }

    @GetMapping("/send/{id}")
    public ResponseEntity sendPda(@PathVariable Long id, @RequestParam(required = false) Integer group, @RequestParam Constants.PdaType type, @RequestParam Boolean bothCurrency) throws Exception {
      emailService.sendPda(id, type, group, bothCurrency);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

  @GetMapping("/updatePdaStatus/{id}/{status}")
  public ResponseEntity updatePdaStatus(@PathVariable Long id, @PathVariable String status, @RequestParam String remark) throws Exception {
    pdaService.updatePdaStatus(id, status, remark);
    return new ResponseEntity<Void>(HttpStatus.OK);
  }

  @GetMapping("/acceptedJobNo")
  public ResponseEntity findAcceptedPda() {
    List<PdaDto> pdaDtos = pdaService.getAcceptedJobNo();
    return ResponseEntity.ok().body(pdaDtos);
  }
}
