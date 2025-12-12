package uk.gov.defra.cdp.trade.demo.interceptor;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import uk.gov.defra.cdp.trade.demo.service.WebIdentityTokenService;

@AllArgsConstructor
public class IpaffsApiClientInterceptor {

    private static final String IPAFFS_TRACING_HEADER = "INS-ConversationId";
    
    private final WebIdentityTokenService webIdentityTokenService;
    
    @Bean
    public RequestInterceptor ipaffsRequestInterceptor() {
        return new RequestInterceptor() {
            String accessToken;

            @Override
            public void apply(RequestTemplate requestTemplate) {

                accessToken = webIdentityTokenService.getWebIdentityToken();
                requestTemplate.header(AUTHORIZATION, "Bearer " + accessToken);
                requestTemplate.header(IPAFFS_TRACING_HEADER, UUID.randomUUID().toString());
            }
        };
    }
}
