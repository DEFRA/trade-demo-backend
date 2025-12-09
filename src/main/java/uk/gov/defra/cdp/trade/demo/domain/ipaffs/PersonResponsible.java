package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonResponsible {
    private String name;
    private String companyId;
    private String companyName;
    private List<String> address;
    private String country;
    private Integer tracesID;
    private String phone;
    private String email;
    private String contactId;
}
