package uk.gov.defra.cdp.trade.demo.service;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.defra.cdp.trade.demo.client.MdmApiClient;
import uk.gov.defra.cdp.trade.demo.client.Token;
import uk.gov.defra.cdp.trade.demo.configuration.MdmConfiguration;
import uk.gov.defra.cdp.trade.demo.domain.mdm.MdmResponse;

@Slf4j
@AllArgsConstructor
@Service
public class MdmService {

    private static final String MDM_API_TRACE_ID_KEY = "x-ms-middleware-request-id";
    
    private final MdmApiClient mdmApiClient;
    private final MdmConfiguration mdmConfiguration;
    
    public MdmResponse getBcps() {

        String ocpApimSubscriptionKey = mdmConfiguration.ocpApimSubscriptionKey;
        
        ResponseEntity<MdmResponse> responseEntity =
            mdmApiClient.getBcps(ocpApimSubscriptionKey);
        Objects.requireNonNull(responseEntity.getHeaders().get(MDM_API_TRACE_ID_KEY))
            .stream().findFirst()
            .ifPresentOrElse(
                mdmApiTraceId -> log.info("MDM trace id for this call is: {}", mdmApiTraceId),
                () -> log.error("No MDM trace id returned")
            );
        
        return responseEntity.getBody();
    }
    
    

}
