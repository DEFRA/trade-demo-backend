package uk.gov.defra.cdp.trade.demo.domain.mdm;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmResponse {

  private MdmData data;
  private LocalDateTime timestamp;

}
