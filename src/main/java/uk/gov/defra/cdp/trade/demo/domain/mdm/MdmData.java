package uk.gov.defra.cdp.trade.demo.domain.mdm;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmData {

  private List<MdmOrganisationDto> result;
  private Integer cursorId;

}
