package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartOne {
    private Commodities commodities;
    private Purpose purpose;
    private String pointOfEntry;
    private MeansOfTransport meansOfTransport;
    private ZonedDateTime submissionDate;
    private User submittedBy;
}
