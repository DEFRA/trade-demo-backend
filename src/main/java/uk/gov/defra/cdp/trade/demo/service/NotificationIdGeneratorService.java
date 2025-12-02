package uk.gov.defra.cdp.trade.demo.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import uk.gov.defra.cdp.trade.demo.domain.IdSequence;

@Component
@RequiredArgsConstructor
public class NotificationIdGeneratorService {

    private final MongoTemplate mongoTemplate;

    public String generateId() {
        LocalDate today = LocalDate.now();
        String dateKey = String.format("CDP.%04d.%02d.%02d",
            today.getYear(),
            today.getMonthValue(),
            today.getDayOfMonth());

        Query query = new Query(Criteria.where("id").is(dateKey));
        Update update = new Update().inc("sequence", 1);
        FindAndModifyOptions options = new FindAndModifyOptions()
            .returnNew(true)
            .upsert(true);

        IdSequence sequence = mongoTemplate.findAndModify(
            query, update, options, IdSequence.class);

        assert sequence != null;
        return String.format("%s.%d", dateKey, sequence.getSequence());
    }
}
