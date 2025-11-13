package uk.gov.defra.cdp.trade.demo.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.cdp.trade.demo.domain.mdm.MdmResponse;
import uk.gov.defra.cdp.trade.demo.service.MdmService;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/mdm")
public class MdmController {
    
    private final MdmService mdmService;
    
    @GetMapping(value = "/bcps")
    public ResponseEntity<MdmResponse> getBcps() {
      
          return ResponseEntity.ok(mdmService.getBcps());
    }
}
