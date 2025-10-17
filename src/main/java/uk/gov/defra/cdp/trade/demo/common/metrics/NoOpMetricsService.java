package uk.gov.defra.cdp.trade.demo.common.metrics;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;

import java.util.Map;

/**
 * No-op implementation of MetricsService for testing.
 * <p>
 * Active ONLY in "test" profile.
 * <p>
 * All metric operations are silent no-ops to avoid:
 * - EMF library overhead in tests
 * - Test pollution from metrics
 * - Dependency on AWS infrastructure
 * <p>
 * USAGE IN TESTS:
 * Simply inject MetricsService and call methods normally.
 * No mocking required - this implementation does nothing.
 */
@Service
@Profile("test")
public class NoOpMetricsService implements MetricsService {

    @Override
    public void counter(String name) {
        // No-op
    }

    @Override
    public void counter(String name, double value) {
        // No-op
    }

    @Override
    public void counter(String name, double value, DimensionSet dimensions) {
        // No-op
    }

    @Override
    public void counterWithContext(String name, double value, DimensionSet dimensions,
                                   Map<String, Object> properties) {
        // No-op
    }
}
