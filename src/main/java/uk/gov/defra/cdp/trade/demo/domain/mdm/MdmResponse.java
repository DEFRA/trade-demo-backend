package uk.gov.defra.cdp.trade.demo.domain.mdm;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MdmResponse {

  private MdmData data;
  private LocalDateTime timestamp;

}
