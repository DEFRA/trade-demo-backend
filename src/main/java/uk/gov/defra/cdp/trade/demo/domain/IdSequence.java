package uk.gov.defra.cdp.trade.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "id_sequences")
public class IdSequence {
    @Id
    private String id;
    private Long sequence;
}

