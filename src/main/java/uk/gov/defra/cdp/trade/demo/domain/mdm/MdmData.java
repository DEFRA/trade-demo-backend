package uk.gov.defra.cdp.trade.demo.domain.mdm;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MdmData {

  private List<MdmOrganisationDto> result;
  private Integer cursorId;

}
