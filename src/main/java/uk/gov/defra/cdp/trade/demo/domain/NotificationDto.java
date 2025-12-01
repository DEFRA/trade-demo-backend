package uk.gov.defra.cdp.trade.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String id;
    private String chedReference;
    private String originCountry;
    private Commodity commodity;
    private String importReason;
    private String internalMarketPurpose;
}
