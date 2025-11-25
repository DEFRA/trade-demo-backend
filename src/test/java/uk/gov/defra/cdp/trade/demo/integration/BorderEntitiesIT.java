package uk.gov.defra.cdp.trade.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import uk.gov.defra.cdp.trade.demo.domain.BcpDto;
import uk.gov.defra.cdp.trade.demo.domain.PoeDto;

@Slf4j
class BorderEntitiesIT extends IntegrationBase {

    private static final String BCP_ENDPOINT = "/border-entities/bcps";
    private static final String POE_ENDPOINT = "/border-entities/poes";

    @BeforeEach
    void setUp() {
        stubMdmResponse();
    }

    @Test
    void getBcps_shouldReturn200() {
        webClient("NoAuth")
            .get()
            .uri(BCP_ENDPOINT)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void getBcps_shouldReturnListOfActiveBcps() {
        EntityExchangeResult<List<BcpDto>> result = webClient("NoAuth")
            .get()
            .uri(BCP_ENDPOINT)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(BcpDto.class)
            .returnResult();

        List<BcpDto> bcps = result.getResponseBody();

        assertThat(bcps).isNotNull().isNotEmpty().hasSize(89);
    }

    @Test
    void getBcps_shouldReturnUniqueResults() {
        EntityExchangeResult<List<BcpDto>> result = webClient("NoAuth")
            .get()
            .uri(BCP_ENDPOINT)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(BcpDto.class)
            .returnResult();

        List<BcpDto> bcps = result.getResponseBody();

        // Check that codes are unique
        long uniqueCodes = bcps.stream()
            .map(BcpDto::getCode)
            .distinct()
            .count();

        assertThat(uniqueCodes).isEqualTo(bcps.size());
    }

    @Test
    void getPoes_shouldReturn200() {
        webClient("NoAuth")
            .get()
            .uri(POE_ENDPOINT)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void getPoes_shouldReturnListOfActivePoes() {
        EntityExchangeResult<List<PoeDto>> result = webClient("NoAuth")
            .get()
            .uri(POE_ENDPOINT)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(PoeDto.class)
            .returnResult();

        List<PoeDto> poes = result.getResponseBody();

        assertThat(poes).isNotNull().isNotEmpty().hasSize(30);
    }

    @Test
    void getPoes_shouldReturnUniqueResults() {
        EntityExchangeResult<List<PoeDto>> result = webClient("NoAuth")
            .get()
            .uri(POE_ENDPOINT)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(PoeDto.class)
            .returnResult();

        List<PoeDto> poes = result.getResponseBody();

        // Check that codes are unique
        long uniqueCodes = poes.stream()
            .map(PoeDto::getCode)
            .distinct()
            .count();

        assertThat(uniqueCodes).isEqualTo(poes.size());
    }

    @Test
    void getBcps_shouldIncludeCodeAndNameOnly() {
        EntityExchangeResult<List<BcpDto>> result = webClient("NoAuth")
            .get()
            .uri(BCP_ENDPOINT)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(BcpDto.class)
            .returnResult();

        List<BcpDto> bcps = result.getResponseBody();

        assertThat(bcps).isNotNull().isNotEmpty().allSatisfy(bcp -> {
            assertThat(bcp.getCode()).isNotNull();
            assertThat(bcp.getName()).isNotNull();
        });
    }

    @Test
    void getPoes_shouldIncludeCodeAndNameOnly() {
        EntityExchangeResult<List<PoeDto>> result = webClient("NoAuth")
            .get()
            .uri(POE_ENDPOINT)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(PoeDto.class)
            .returnResult();

        List<PoeDto> poes = result.getResponseBody();

        assertThat(poes).isNotNull().isNotEmpty().allSatisfy(poe -> {
            assertThat(poe.getCode()).isNotNull();
            assertThat(poe.getName()).isNotNull();
        });
    }
}
