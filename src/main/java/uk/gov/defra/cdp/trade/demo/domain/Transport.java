package uk.gov.defra.cdp.trade.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transport {
    
    private String bcpCode;
    private String transportToBcp;
    private String vehicleId;

}
