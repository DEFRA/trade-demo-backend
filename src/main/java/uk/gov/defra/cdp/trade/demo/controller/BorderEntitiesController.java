package uk.gov.defra.cdp.trade.demo.controller;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.cdp.trade.demo.domain.BcpDto;
import uk.gov.defra.cdp.trade.demo.domain.PoeDto;
import uk.gov.defra.cdp.trade.demo.domain.mdm.MdmOrganisationDto;
import uk.gov.defra.cdp.trade.demo.domain.mdm.MdmResponse;
import uk.gov.defra.cdp.trade.demo.service.MdmService;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/border-entities")
public class BorderEntitiesController {

  private static final String ACTIVE_STATUS = "active";

  private final MdmService mdmService;

  @GetMapping(value = "/bcps")
  public ResponseEntity<List<BcpDto>> getBcps() {
    log.debug("Fetching all active BCPs");

    MdmResponse mdmResponse = mdmService.getBcps();

    if (mdmResponse == null || mdmResponse.getData() == null || mdmResponse.getData().getResult() == null) {
      log.warn("No BCP data returned from MDM service");
      return ResponseEntity.ok(Collections.emptyList());
    }

    List<BcpDto> activeBcps = mdmResponse.getData().getResult().stream()
        .filter(org -> org.getStatus() != null && ACTIVE_STATUS.equalsIgnoreCase(org.getStatus().getCode()))
        .map(org -> BcpDto.builder()
            .code(org.getCode())
            .name(org.getName())
            .build())
        .filter(bcp -> bcp.getCode() != null)
        .distinct()
        .toList();

    log.debug("Returning {} active BCPs", activeBcps.size());
    return ResponseEntity.ok(activeBcps);
  }

  @GetMapping(value = "/poes")
  public ResponseEntity<List<PoeDto>> getPoes() {
    log.debug("Fetching all active POEs");

    MdmResponse mdmResponse = mdmService.getBcps();

    if (mdmResponse == null || mdmResponse.getData() == null || mdmResponse.getData().getResult() == null) {
      log.warn("No data returned from MDM service");
      return ResponseEntity.ok(Collections.emptyList());
    }

    List<PoeDto> activePoes = mdmResponse.getData().getResult().stream()
        .filter(Objects::nonNull)
        .map(MdmOrganisationDto::getPointsOfEntry)
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .filter(poe -> poe.getStatus() != null && ACTIVE_STATUS.equalsIgnoreCase(poe.getStatus().getCode()))
        .map(poe -> PoeDto.builder()
            .code(poe.getCode())
            .name(poe.getName())
            .build())
        .filter(poe -> poe.getCode() != null)
        .distinct()
        .toList();

    log.debug("Returning {} active POEs", activePoes.size());
    return ResponseEntity.ok(activePoes);
  }
}
