package uk.gov.defra.cdp.trade.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.IpaffsNotification;
import uk.gov.defra.cdp.trade.demo.interceptor.IpaffsApiClientInterceptor;

@SuppressWarnings("unused")
@Profile("!local & !integration-test")
@FeignClient(
    name = "ipaffs-client",
    url="${ipaffs.api.baseUrl}",
    configuration = IpaffsApiClientInterceptor.class
)
public interface IpaffsNotificationClientImpl extends IpaffsNotificationClient {
    
    @Override
    @PostMapping("/notificationapi/vnet/notificationapi")
    ResponseEntity<String> submitNotification(
        @RequestBody IpaffsNotification ipaffsNotification, 
        @RequestHeader(INS_CONVERSATION_ID_HEADER_KEY) String conversationId
    );

}
