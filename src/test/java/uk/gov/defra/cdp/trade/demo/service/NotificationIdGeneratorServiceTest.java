package uk.gov.defra.cdp.trade.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.defra.cdp.trade.demo.domain.IdSequence;

@ExtendWith(MockitoExtension.class)
class NotificationIdGeneratorServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private NotificationIdGeneratorService service;

    @Captor
    private ArgumentCaptor<Query> queryCaptor;

    @Captor
    private ArgumentCaptor<Update> updateCaptor;

    @Captor
    private ArgumentCaptor<FindAndModifyOptions> optionsCaptor;

    private String expectedDateKey;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        expectedDateKey = String.format("CDP.%04d.%02d.%02d",
            today.getYear(),
            today.getMonthValue(),
            today.getDayOfMonth());

        service = new NotificationIdGeneratorService(mongoTemplate);
    }

    @Test
    void shouldGenerateIdWithCorrectMongoOperationAndFormat() {
        // Given
        IdSequence sequence = new IdSequence(expectedDateKey, 42L);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
            any(FindAndModifyOptions.class), eq(IdSequence.class)))
            .thenReturn(sequence);

        // When
        String generatedId = service.generateId();

        // Then
        verify(mongoTemplate).findAndModify(
            queryCaptor.capture(),
            updateCaptor.capture(),
            optionsCaptor.capture(),
            eq(IdSequence.class)
        );

        // Verify Query: should query by the date key
        Query capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery.getQueryObject()).containsEntry("id", expectedDateKey);

        // Verify Update: should increment sequence by 1
        Update capturedUpdate = updateCaptor.getValue();
        assertThat(capturedUpdate.getUpdateObject().get("$inc")).isNotNull();
        assertThat(capturedUpdate.getUpdateObject().get("$inc").toString())
            .contains("sequence")
            .contains("1");

        // Verify Options: should use returnNew and upsert
        FindAndModifyOptions capturedOptions = optionsCaptor.getValue();
        assertThat(capturedOptions.isReturnNew()).isTrue();
        assertThat(capturedOptions.isUpsert()).isTrue();

        // Verify returned ID format: CDP.YYYY.MM.DD.sequence
        assertThat(generatedId).isEqualTo(expectedDateKey + ".42");
    }

    @Test
    void shouldGenerateMultipleUniqueIdsInSequence() {
        // Given
        IdSequence sequence1 = new IdSequence(expectedDateKey, 1L);
        IdSequence sequence2 = new IdSequence(expectedDateKey, 2L);
        IdSequence sequence3 = new IdSequence(expectedDateKey, 3L);

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
            any(FindAndModifyOptions.class), eq(IdSequence.class)))
            .thenReturn(sequence1, sequence2, sequence3);

        // When
        String id1 = service.generateId();
        String id2 = service.generateId();
        String id3 = service.generateId();

        // Then
        assertThat(id1).isEqualTo(expectedDateKey + ".1");
        assertThat(id2).isEqualTo(expectedDateKey + ".2");
        assertThat(id3).isEqualTo(expectedDateKey + ".3");
    }
}
