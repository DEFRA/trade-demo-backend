package uk.gov.defra.cdp.trade.demo.domain.mdm;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MdmPoeDto {

  private UUID pointOfEntryUUID;
  @EqualsAndHashCode.Include
  private String code;
  private String name;
  private MdmStatus status;

  public MdmPoe toEntity() {
    return MdmPoe.builder()
        .pointOfEntryUUID(pointOfEntryUUID)
        .code(code)
        .name(name)
        .status(status != null ? status.getCode() : null)
        .build();
  }
}
