package uk.gov.defra.cdp.trade.demo.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.defra.cdp.trade.demo.configuration.MdmApiClientInterceptorConfig;
import uk.gov.defra.cdp.trade.demo.domain.mdm.MdmResponse;

@FeignClient(
    name = "mdm-client", 
    url="${mdm-service.url}", 
    configuration = MdmApiClientInterceptorConfig.class
)
public interface MdmApiClient {
    
    String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";

    @GetMapping(value = "/mdm/trade/bcp/bcps")
    ResponseEntity<MdmResponse> getBcps(
        @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey
    );
}
