package uk.gov.defra.cdp.trade.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.defra.cdp.trade.demo.domain.Example;
import uk.gov.defra.cdp.trade.demo.service.ExampleService;

@SpringBootTest
@TestPropertySource(properties = {
    "management.metrics.enabled=true",
    "spring.data.mongodb.uri=mongodb://localhost:27017/test"
})
class ExampleControllerMetricsTest {

    @Autowired
    private ExampleController exampleController;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private ExampleService exampleService;

    @Test
    void findAll_shouldRecordTimedMetric() {
        // Given
        when(exampleService.findAll()).thenReturn(Collections.emptyList());
        meterRegistry.clear();

        // When
        exampleController.findAll();

        // Then
        Timer timer = meterRegistry.find("controller.getAllExampleEntities.time").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void findAll_shouldRecordCountedMetric() {
        // Given
        when(exampleService.findAll()).thenReturn(Collections.emptyList());
        meterRegistry.clear();

        // When
        exampleController.findAll();

        // Then
        assertThat(meterRegistry.find("controller.getAllExampleEntities.count").counter()).isNotNull();
        assertThat(meterRegistry.find("controller.getAllExampleEntities.count").counter().count()).isEqualTo(1);
    }

    @Test
    void create_shouldRecordTimedMetric() {
        // Given
        Example example = new Example();
        example.setName("test");
        when(exampleService.create(any(Example.class))).thenReturn(example);
        meterRegistry.clear();

        // When
        exampleController.create(example);

        // Then
        Timer timer = meterRegistry.find("controller.PostExampleEntity").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void findAll_shouldIncrementMetricsOnMultipleInvocations() {
        // Given
        when(exampleService.findAll()).thenReturn(Collections.emptyList());
        meterRegistry.clear();

        // When
        exampleController.findAll();
        exampleController.findAll();
        exampleController.findAll();

        // Then
        Timer timer = meterRegistry.find("controller.getAllExampleEntities.time").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(3);

        assertThat(meterRegistry.find("controller.getAllExampleEntities.count").counter()).isNotNull();
        assertThat(meterRegistry.find("controller.getAllExampleEntities.count").counter().count()).isEqualTo(3);
    }
}
