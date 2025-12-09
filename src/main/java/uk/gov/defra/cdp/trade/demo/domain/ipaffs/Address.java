package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String city;
    private String postalZipCode;
    private String countryISOCode;
    private String telephone;
    private String email;
}
