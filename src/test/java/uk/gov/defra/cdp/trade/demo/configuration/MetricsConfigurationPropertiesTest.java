package uk.gov.defra.cdp.trade.demo.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.defra.cdp.trade.demo.service.EmfMetricsPublisher;

@SpringBootTest
@TestPropertySource(properties = {
    "management.metrics.enabled=true",
    "aws.emf.namespace=test-namespace",
    "spring.data.mongodb.uri=mongodb://localhost:27017/test"
})
class MetricsConfigurationPropertiesTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void whenMetricsEnabled_emfMetricsPublisherBeanShouldBeCreated() {
        // Then
        assertThat(applicationContext.containsBean("emfMetricsPublisher")).isTrue();
        assertThat(applicationContext.getBean(EmfMetricsPublisher.class)).isNotNull();
    }

    @Test
    void whenMetricsEnabled_specificMetricsShouldBeConfigured() {
        // Given
        io.micrometer.core.instrument.MeterRegistry registry =
            applicationContext.getBean(io.micrometer.core.instrument.MeterRegistry.class);

        // When
        registry.counter("controller.test").increment();

        // Then
        assertThat(registry.find("controller.test").counter()).isNotNull();
    }

    @SpringBootTest
    @TestPropertySource(properties = {
        "management.metrics.enabled=false",
        "spring.data.mongodb.uri=mongodb://localhost:27017/test"
    })
    static class MetricsDisabledTest {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void whenMetricsDisabled_emfMetricsPublisherBeanShouldNotBeCreated() {
            // Then
            assertThat(applicationContext.containsBean("emfMetricsPublisher")).isFalse();
        }
    }
}
