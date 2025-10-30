package uk.gov.defra.cdp.trade.demo.metric;

import org.springframework.stereotype.Component;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.StorageResolution;
import software.amazon.cloudwatchlogs.emf.model.Unit;

@Component
public class MetricProducer {

    public void emitMetric(Environment environment)
        throws InvalidDimensionException, InvalidMetricException, DimensionSetExceededException {
        MetricsLogger logger = new MetricsLogger(environment);
        logger.setDimensions(DimensionSet.of("Operation", "Agent"));
        logger.putMetric("ExampleMetric", 100, Unit.MILLISECONDS);
        logger.putMetric("ExampleHighResolutionMetric", 10, Unit.MILLISECONDS, StorageResolution.HIGH);
        logger.putProperty("RequestId", "422b1569-16f6-4a03-b8f0-fe3fd9b100f8");
        logger.flush();
    }
}
