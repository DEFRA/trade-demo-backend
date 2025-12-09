package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Party {
    private String id;
    private String type;
    private String status;
    private String companyName;
    private String individualName;
    private String approvalNumber;
    private Address address;
    private Integer tracesId;
}
