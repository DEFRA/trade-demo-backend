package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplementParameterSet {
    private String uniqueComplementID;
    private Integer complementID;
    private String speciesID;
    private List<KeyDataPair> keyDataPair;
}
