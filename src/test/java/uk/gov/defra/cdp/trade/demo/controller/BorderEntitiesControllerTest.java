package uk.gov.defra.cdp.trade.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.cdp.trade.demo.domain.BcpDto;
import uk.gov.defra.cdp.trade.demo.domain.PoeDto;
import uk.gov.defra.cdp.trade.demo.domain.mdm.MdmData;
import uk.gov.defra.cdp.trade.demo.domain.mdm.MdmOrganisationDto;
import uk.gov.defra.cdp.trade.demo.domain.mdm.MdmPoeDto;
import uk.gov.defra.cdp.trade.demo.domain.mdm.MdmResponse;
import uk.gov.defra.cdp.trade.demo.domain.mdm.MdmStatus;
import uk.gov.defra.cdp.trade.demo.service.MdmService;

@ExtendWith(MockitoExtension.class)
class BorderEntitiesControllerTest {

  @Mock
  private MdmService mdmService;

  private BorderEntitiesController controller;

  private MdmResponse mockMdmResponse;

  @BeforeEach
  void setUp() {
    mockMdmResponse = createMockMdmResponse();
    controller = new BorderEntitiesController(mdmService);
  }

  @Test
  void getBcps_shouldReturnActiveBcpsOnly() {
    when(mdmService.getBcps()).thenReturn(mockMdmResponse);

    ResponseEntity<List<BcpDto>> response = controller.getBcps();

    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody()).hasSize(2),
        () -> assertThat(response.getBody()).extracting(BcpDto::getCode)
            .containsExactlyInAnyOrder("BCP001", "BCP002"),
        () -> assertThat(response.getBody()).extracting(BcpDto::getName)
            .containsExactlyInAnyOrder("Active BCP 1", "Active BCP 2")
    );
  }

  @Test
  void getBcps_shouldFilterOutInactiveBcps() {
    when(mdmService.getBcps()).thenReturn(mockMdmResponse);

    ResponseEntity<List<BcpDto>> response = controller.getBcps();

    assertThat(response.getBody())
        .extracting(BcpDto::getCode)
        .isNotEmpty()
        .doesNotContain("BCP003"); // Inactive BCP should be filtered out
  }

  @Test
  void getBcps_shouldReturnEmptyListWhenNoData() {
    MdmResponse emptyResponse = MdmResponse.builder()
        .data(MdmData.builder().result(Collections.emptyList()).build())
        .timestamp(LocalDateTime.now())
        .build();
    when(mdmService.getBcps()).thenReturn(emptyResponse);

    ResponseEntity<List<BcpDto>> response = controller.getBcps();

    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).isEmpty()
    );
  }

  @Test
  void getBcps_shouldReturnEmptyListWhenMdmResponseIsNull() {
    when(mdmService.getBcps()).thenReturn(null);

    ResponseEntity<List<BcpDto>> response = controller.getBcps();

    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).isEmpty()
    );
  }

  @Test
  void getBcps_shouldReturnUniqueResults() {
    // Create response with duplicate BCPs
    MdmOrganisationDto duplicateBcp = createBcp("BCP001", "Active BCP 1", "active");
    MdmData dataWithDuplicates = MdmData.builder()
        .result(Arrays.asList(duplicateBcp, duplicateBcp))
        .build();
    MdmResponse responseWithDuplicates = MdmResponse.builder()
        .data(dataWithDuplicates)
        .timestamp(LocalDateTime.now())
        .build();

    when(mdmService.getBcps()).thenReturn(responseWithDuplicates);

    ResponseEntity<List<BcpDto>> response = controller.getBcps();

    assertThat(response.getBody())
        .hasSize(1)
        .extracting(BcpDto::getCode)
        .containsExactly("BCP001");
  }

  @Test
  void getPoes_shouldReturnActivePoesOnly() {
    when(mdmService.getBcps()).thenReturn(mockMdmResponse);

    ResponseEntity<List<PoeDto>> response = controller.getPoes();

    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody()).hasSize(2),
        () -> assertThat(response.getBody()).extracting(PoeDto::getCode)
            .containsExactlyInAnyOrder("POE001", "POE002"),
        () -> assertThat(response.getBody()).extracting(PoeDto::getName)
            .containsExactlyInAnyOrder("Active POE 1", "Active POE 2")
    );
  }

  @Test
  void getPoes_shouldFilterOutInactivePoes() {
    when(mdmService.getBcps()).thenReturn(mockMdmResponse);

    ResponseEntity<List<PoeDto>> response = controller.getPoes();

    assertThat(response.getBody())
        .extracting(PoeDto::getCode)
        .isNotEmpty()
        .doesNotContain("POE003"); // Inactive POE should be filtered out
  }

  @Test
  void getPoes_shouldReturnEmptyListWhenNoData() {
    MdmResponse emptyResponse = MdmResponse.builder()
        .data(MdmData.builder().result(Collections.emptyList()).build())
        .timestamp(LocalDateTime.now())
        .build();
    when(mdmService.getBcps()).thenReturn(emptyResponse);

    ResponseEntity<List<PoeDto>> response = controller.getPoes();

    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).isEmpty()
    );
  }

  @Test
  void getPoes_shouldReturnEmptyListWhenMdmResponseIsNull() {
    when(mdmService.getBcps()).thenReturn(null);

    ResponseEntity<List<PoeDto>> response = controller.getPoes();

    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).isEmpty()
    );
  }

  @Test
  void getPoes_shouldFlattenPoesFromMultipleBcps() {
    when(mdmService.getBcps()).thenReturn(mockMdmResponse);

    ResponseEntity<List<PoeDto>> response = controller.getPoes();

    // POEs come from different BCPs in the mock data
    assertThat(response.getBody()).hasSize(2);
  }

  @Test
  void getPoes_shouldReturnUniqueResults() {
    // Create POE that appears in multiple BCPs
    MdmPoeDto duplicatePoe = createPoe("POE001", "Active POE 1", "active");
    MdmOrganisationDto bcp1 = createBcpWithPoes("BCP001", "BCP 1", "active",
        Collections.singletonList(duplicatePoe));
    MdmOrganisationDto bcp2 = createBcpWithPoes("BCP002", "BCP 2", "active",
        Collections.singletonList(duplicatePoe));

    MdmData dataWithDuplicates = MdmData.builder()
        .result(Arrays.asList(bcp1, bcp2))
        .build();
    MdmResponse responseWithDuplicates = MdmResponse.builder()
        .data(dataWithDuplicates)
        .timestamp(LocalDateTime.now())
        .build();

    when(mdmService.getBcps()).thenReturn(responseWithDuplicates);

    ResponseEntity<List<PoeDto>> response = controller.getPoes();

    assertThat(response.getBody())
        .hasSize(1)
        .extracting(PoeDto::getCode)
        .containsExactly("POE001");
  }

  @Test
  void getPoes_shouldFilterByStatusCaseInsensitive() {
    // Create POE with "ACTIVE" in uppercase
    MdmPoeDto uppercasePoe = createPoe("POE004", "Uppercase POE", "ACTIVE");
    MdmOrganisationDto bcp = createBcpWithPoes("BCP004", "BCP 4", "active",
        Collections.singletonList(uppercasePoe));

    MdmData data = MdmData.builder()
        .result(Collections.singletonList(bcp))
        .build();
    MdmResponse response = MdmResponse.builder()
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();

    when(mdmService.getBcps()).thenReturn(response);

    ResponseEntity<List<PoeDto>> result = controller.getPoes();

    assertThat(result.getBody())
        .hasSize(1)
        .extracting(PoeDto::getCode)
        .containsExactly("POE004");
  }

  @Test
  void getBcps_shouldFilterByStatusCaseInsensitive() {
    // Create BCP with "ACTIVE" in uppercase
    MdmOrganisationDto uppercaseBcp = createBcp("BCP004", "Uppercase BCP", "ACTIVE");

    MdmData data = MdmData.builder()
        .result(Collections.singletonList(uppercaseBcp))
        .build();
    MdmResponse response = MdmResponse.builder()
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();

    when(mdmService.getBcps()).thenReturn(response);

    ResponseEntity<List<BcpDto>> result = controller.getBcps();

    assertThat(result.getBody())
        .hasSize(1)
        .extracting(BcpDto::getCode)
        .containsExactly("BCP004");
  }

  // Helper methods to create test data
  private MdmResponse createMockMdmResponse() {
    MdmOrganisationDto activeBcp1 = createBcpWithPoes("BCP001", "Active BCP 1", "active",
        Collections.singletonList(createPoe("POE001", "Active POE 1", "active")));

    MdmOrganisationDto activeBcp2 = createBcpWithPoes("BCP002", "Active BCP 2", "active",
        Collections.singletonList(createPoe("POE002", "Active POE 2", "active")));

    MdmOrganisationDto inactiveBcp = createBcpWithPoes("BCP003", "Inactive BCP", "inactive",
        Collections.singletonList(createPoe("POE003", "Inactive POE", "inactive")));

    List<MdmOrganisationDto> organisations = Arrays.asList(activeBcp1, activeBcp2, inactiveBcp);

    MdmData data = MdmData.builder()
        .result(organisations)
        .cursorId(0)
        .build();

    return MdmResponse.builder()
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }

  private MdmOrganisationDto createBcp(String code, String name, String statusCode) {
    return createBcpWithPoes(code, name, statusCode, Collections.emptyList());
  }

  private MdmOrganisationDto createBcpWithPoes(String code, String name, String statusCode,
      List<MdmPoeDto> poes) {
    MdmStatus status = new MdmStatus();
    status.setCode(statusCode);

    return MdmOrganisationDto.builder()
        .organisationUUID(UUID.randomUUID())
        .code(code)
        .name(name)
        .status(status)
        .pointsOfEntry(poes)
        .build();
  }

  private MdmPoeDto createPoe(String code, String name, String statusCode) {
    MdmStatus status = new MdmStatus();
    status.setCode(statusCode);

    return MdmPoeDto.builder()
        .pointOfEntryUUID(UUID.randomUUID())
        .code(code)
        .name(name)
        .status(status)
        .build();
  }
}
