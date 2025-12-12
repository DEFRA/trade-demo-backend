package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalReference {
    private String system;
    private String reference;
    private Boolean exactMatch;
    private Boolean verifiedByImporter;
    private Boolean verifiedByInspector;
}
