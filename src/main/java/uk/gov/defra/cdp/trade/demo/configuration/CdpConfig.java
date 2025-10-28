package uk.gov.defra.cdp.trade.demo.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cdp")
public class CdpConfig {

    private String proxyUrl;
    
    private String serviceVersion;

    private String tracingHeaderName;
    
}
