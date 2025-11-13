package uk.gov.defra.cdp.trade.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.JsonBody;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import uk.gov.defra.cdp.trade.demo.domain.mdm.MdmResponse;

@Slf4j
class MdmEndPointsIT extends IntegrationBase {

    private static final String BCP_ENDPOINT = "/mdm/bcps";
    
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        stubMdmResponse();
    }

    @Test
    void should_status_200_on_successful_mdm_authentication() {

        webClient("NoAuth")
            .get()
            .uri(BCP_ENDPOINT)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void should_return_valid_bcp_response() {

        EntityExchangeResult<byte[]> result = webClient("NoAuth")
            .get()
            .uri(BCP_ENDPOINT)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .returnResult();

        MdmResponse mdmResponse = getResponseAsObject(result.getResponseBody(), MdmResponse.class);
            
        assertAll(() -> {
            assertNotNull(mdmResponse);
            assertNotNull(mdmResponse.getData());
            assertThat(mdmResponse.getData().getResult().size()).isGreaterThanOrEqualTo(1);
        });
    }

    private void stubMdmResponse() {
        usingStub()
            .when(request()
                .withMethod("GET")
                .withPath("/mdm-service/mdm/trade/bcp/bcps")
                .withHeader("Ocp-Apim-Subscription-Key", "test")
            )
            .respond(response().withBody(
                getJsonFromFile("integration/mdm-response.json")
            ).withHeader("x-ms-middleware-request-id", "trace-id"));
    }

    @SneakyThrows
    JsonBody getJsonFromFile(String filename) {
        return new JsonBody(
            Files.readString(Paths.get(getClass().getClassLoader().getResource(filename).toURI())));
    }
}
