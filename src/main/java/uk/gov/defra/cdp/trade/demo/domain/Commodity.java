package uk.gov.defra.cdp.trade.demo.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Commodity {
    
    private String code;
    private String description;
    private List<Species> species;

}
