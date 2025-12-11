package uk.gov.defra.cdp.trade.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import uk.gov.defra.cdp.trade.demo.interceptor.IpaffsApiClientInterceptor;

@FeignClient(
    name = "ipaffs-client",
    url="${ipaffs.api.baseUrl}",
    configuration = IpaffsApiClientInterceptor.class
)
public interface IpaffsApiClient {
    
    @PostMapping("/notificationapi/snd/protected/notifications")
    void submitNotification();

}
