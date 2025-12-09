package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IpaffsNotification {
    private Integer version;
    private String type;
    private String status;
    private Boolean isHighRiskEuImport;
    private List<ExternalReference> externalReferences;
    private PartOne partOne;
    private String etag;
    private String riskDecisionLockingTime;
    private Boolean isRiskDecisionLocked;
    private Integer chedTypeVersion;
}
