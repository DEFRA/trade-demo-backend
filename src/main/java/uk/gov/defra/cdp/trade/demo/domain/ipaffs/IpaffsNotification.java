package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IpaffsNotification {
    private String type;
    private String status;
    private List<ExternalReference> externalReferences;
    private PartOne partOne;
    private LocalDateTime submissionDate;
    private User submittedBy;
}
