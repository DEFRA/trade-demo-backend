package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String displayName;
    private String userId;
}
