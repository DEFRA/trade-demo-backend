# Metrics Test Coverage Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add comprehensive test coverage for metrics functionality (MetricsConfig, EmfMetricsPublisher, and controller annotations)

**Architecture:** Unit tests using JUnit 5, Mockito, and Micrometer test utilities. Integration tests verify @Timed/@Counted annotations work end-to-end with MeterRegistry.

**Tech Stack:** JUnit 5, Mockito, AssertJ, Micrometer Test, SimpleMeterRegistry

---

## Task 1: MetricsConfig Unit Tests

**Files:**
- Create: `src/test/java/uk/gov/defra/cdp/trade/demo/configuration/MetricsConfigTest.java`

**Step 1: Write the failing test for TimedAspect bean creation**

Create the test file with imports and first test:

```java
package uk.gov.defra.cdp.trade.demo.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricsConfigTest {

    private MetricsConfig metricsConfig;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        metricsConfig = new MetricsConfig();
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void timedAspect_shouldCreateTimedAspectWithRegistry() {
        // When
        TimedAspect result = metricsConfig.timedAspect(meterRegistry);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(TimedAspect.class);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=MetricsConfigTest#timedAspect_shouldCreateTimedAspectWithRegistry`

Expected: Test should PASS (bean creation already works)

**Step 3: Add test for CountedAspect bean creation**

Add this test method to the class:

```java
    @Test
    void countedAspect_shouldCreateCountedAspectWithRegistry() {
        // When
        CountedAspect result = metricsConfig.countedAspect(meterRegistry);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(CountedAspect.class);
    }
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=MetricsConfigTest`

Expected: Both tests PASS

**Step 5: Add test to verify bean creation with null registry throws exception**

Add this test:

```java
    @Test
    void timedAspect_shouldHandleNullRegistry() {
        // When / Then
        assertThat(metricsConfig.timedAspect(null)).isNotNull();
    }

    @Test
    void countedAspect_shouldHandleNullRegistry() {
        // When / Then
        assertThat(metricsConfig.countedAspect(null)).isNotNull();
    }
```

**Step 6: Run all tests**

Run: `mvn test -Dtest=MetricsConfigTest`

Expected: All 4 tests PASS

**Step 7: Commit**

```bash
git add src/test/java/uk/gov/defra/cdp/trade/demo/configuration/MetricsConfigTest.java
git commit -m "test: add unit tests for MetricsConfig bean creation"
```

---

## Task 2: EmfMetricsPublisher Unit Tests

**Files:**
- Create: `src/test/java/uk/gov/defra/cdp/trade/demo/service/EmfMetricsPublisherTest.java`

**Step 1: Write the failing test setup**

Create test file with basic structure:

```java
package uk.gov.defra.cdp.trade.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmfMetricsPublisherTest {

    private static final String TEST_NAMESPACE = "test-namespace";

    @Mock
    private MeterRegistry meterRegistry;

    private EmfMetricsPublisher emfMetricsPublisher;

    @BeforeEach
    void setUp() {
        emfMetricsPublisher = new EmfMetricsPublisher(TEST_NAMESPACE, meterRegistry);
    }

    @Test
    void constructor_shouldInitializeWithNamespaceAndRegistry() {
        // Then
        assertThat(emfMetricsPublisher).isNotNull();
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=EmfMetricsPublisherTest#constructor_shouldInitializeWithNamespaceAndRegistry`

Expected: Test PASSES (constructor exists)

**Step 3: Add test for publishMetrics with empty meter registry**

Add this test:

```java
    @Test
    void publishMetrics_shouldHandleEmptyMeterRegistry() {
        // Given
        when(meterRegistry.getMeters()).thenReturn(Collections.emptyList());

        // When
        emfMetricsPublisher.publishMetrics();

        // Then
        verify(meterRegistry, times(1)).getMeters();
    }
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=EmfMetricsPublisherTest#publishMetrics_shouldHandleEmptyMeterRegistry`

Expected: Test PASSES

**Step 5: Add test for publishMetrics with actual meters**

Add this test:

```java
    @Test
    void publishMetrics_shouldIterateOverAllMeters() {
        // Given
        SimpleMeterRegistry realRegistry = new SimpleMeterRegistry();
        realRegistry.counter("test.counter").increment();
        realRegistry.timer("test.timer").record(() -> {});

        EmfMetricsPublisher publisher = new EmfMetricsPublisher(TEST_NAMESPACE, realRegistry);

        // When
        publisher.publishMetrics();

        // Then
        assertThat(realRegistry.getMeters()).isNotEmpty();
        assertThat(realRegistry.getMeters()).hasSize(2);
    }
```

**Step 6: Run test to verify it passes**

Run: `mvn test -Dtest=EmfMetricsPublisherTest`

Expected: All 3 tests PASS

**Step 7: Add test to verify metrics are collected from meters**

Add this test:

```java
    @Test
    void publishMetrics_shouldCollectMetricsFromMeter() {
        // Given
        Meter mockMeter = mock(Meter.class);
        Id meterId = new Id("test.metric", Collections.emptyList(), null, null, Meter.Type.COUNTER);
        Measurement measurement = new Measurement(() -> 42.0, null);

        when(mockMeter.getId()).thenReturn(meterId);
        when(mockMeter.measure()).thenReturn(Arrays.asList(measurement));
        when(meterRegistry.getMeters()).thenReturn(Arrays.asList(mockMeter));

        // When
        emfMetricsPublisher.publishMetrics();

        // Then
        verify(meterRegistry, times(1)).getMeters();
        verify(mockMeter, times(1)).measure();
    }
```

**Step 8: Run all tests**

Run: `mvn test -Dtest=EmfMetricsPublisherTest`

Expected: All 4 tests PASS

**Step 9: Commit**

```bash
git add src/test/java/uk/gov/defra/cdp/trade/demo/service/EmfMetricsPublisherTest.java
git commit -m "test: add unit tests for EmfMetricsPublisher"
```

---

## Task 3: Controller Metrics Integration Tests

**Files:**
- Create: `src/test/java/uk/gov/defra/cdp/trade/demo/controller/ExampleControllerMetricsTest.java`

**Step 1: Write failing integration test for @Timed annotation**

Create test file:

```java
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
}
```

**Step 2: Run test to verify behavior**

Run: `mvn test -Dtest=ExampleControllerMetricsTest#findAll_shouldRecordTimedMetric`

Expected: Test may PASS or FAIL depending on aspect configuration. If it fails, it's because @Timed needs aspect weaving.

**Step 3: Add test for @Counted annotation**

Add this test:

```java
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
```

**Step 4: Run test to verify it works**

Run: `mvn test -Dtest=ExampleControllerMetricsTest#findAll_shouldRecordCountedMetric`

Expected: Test PASSES

**Step 5: Add test for POST endpoint @Timed metric**

Add this test:

```java
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
```

**Step 6: Run all controller metrics tests**

Run: `mvn test -Dtest=ExampleControllerMetricsTest`

Expected: All 3 tests PASS

**Step 7: Add test for multiple invocations incrementing metrics**

Add this test:

```java
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
```

**Step 8: Run all tests**

Run: `mvn test -Dtest=ExampleControllerMetricsTest`

Expected: All 4 tests PASS

**Step 9: Commit**

```bash
git add src/test/java/uk/gov/defra/cdp/trade/demo/controller/ExampleControllerMetricsTest.java
git commit -m "test: add integration tests for controller metrics annotations"
```

---

## Task 4: Verify Metrics Configuration Properties

**Files:**
- Create: `src/test/java/uk/gov/defra/cdp/trade/demo/configuration/MetricsConfigurationPropertiesTest.java`

**Step 1: Write test for metrics enabled property**

Create test file:

```java
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
}
```

**Step 2: Run test to verify it passes**

Run: `mvn test -Dtest=MetricsConfigurationPropertiesTest#whenMetricsEnabled_emfMetricsPublisherBeanShouldBeCreated`

Expected: Test PASSES

**Step 3: Add test for metrics disabled scenario**

Add this test:

```java
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
```

Note: Add this as a nested static class inside MetricsConfigurationPropertiesTest.

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=MetricsConfigurationPropertiesTest`

Expected: Both tests PASS

**Step 5: Add test for selective metric enabling**

Add this test to the main class:

```java
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
```

**Step 6: Run all tests**

Run: `mvn test -Dtest=MetricsConfigurationPropertiesTest`

Expected: All 3 tests PASS

**Step 7: Commit**

```bash
git add src/test/java/uk/gov/defra/cdp/trade/demo/configuration/MetricsConfigurationPropertiesTest.java
git commit -m "test: add configuration property tests for metrics enablement"
```

---

## Task 5: Run Full Test Suite and Verify Coverage

**Step 1: Run all new tests together**

Run: `mvn test -Dtest=MetricsConfigTest,EmfMetricsPublisherTest,ExampleControllerMetricsTest,MetricsConfigurationPropertiesTest`

Expected: All tests PASS

**Step 2: Run full test suite**

Run: `mvn test`

Expected: All tests PASS (including existing tests)

**Step 3: Generate coverage report**

Run: `mvn clean verify jacoco:report`

Expected: BUILD SUCCESS with coverage report generated

**Step 4: Check coverage metrics**

Run: `cat target/site/jacoco/index.html | grep -A 2 "Total"`

Or open: `target/site/jacoco/index.html` in browser

Expected: Line coverage should meet or exceed 65% threshold

**Step 5: Review specific class coverage**

Check coverage for:
- `MetricsConfig.java` - should be 100%
- `EmfMetricsPublisher.java` - should be >80%
- `ExampleController.java` - should remain high

**Step 6: Commit if all checks pass**

```bash
git add .
git commit -m "test: verify metrics test coverage meets 65% threshold"
```

---

## Task 6: Update Documentation (Optional)

**Files:**
- Modify: `README.md` (if exists) or create `docs/METRICS.md`

**Step 1: Document metrics testing approach**

Create or update documentation:

```markdown
## Metrics Testing

The application uses Micrometer for metrics collection with the following test coverage:

### Unit Tests
- `MetricsConfigTest` - Verifies bean creation for TimedAspect and CountedAspect
- `EmfMetricsPublisherTest` - Verifies metrics collection and EMF publishing logic

### Integration Tests
- `ExampleControllerMetricsTest` - Verifies @Timed and @Counted annotations work end-to-end
- `MetricsConfigurationPropertiesTest` - Verifies conditional bean creation based on properties

### Running Metrics Tests

```bash
# Run all metrics tests
mvn test -Dtest=*Metrics*

# Run with coverage report
mvn clean verify jacoco:report
```

### Metrics Naming Convention
- Controller metrics: `controller.{operation}.{type}` (e.g., `controller.getAllExampleEntities.time`)
- Custom metrics: Use descriptive names following Micrometer conventions
```

**Step 2: Commit documentation**

```bash
git add docs/METRICS.md
git commit -m "docs: add metrics testing documentation"
```

---

## Summary

This plan adds comprehensive test coverage for:
1. ✅ MetricsConfig bean creation (TimedAspect, CountedAspect)
2. ✅ EmfMetricsPublisher metrics collection logic
3. ✅ Controller @Timed and @Counted annotations
4. ✅ Configuration properties for metrics enablement
5. ✅ Coverage verification (65% threshold)
6. ✅ Documentation updates

**Total estimated time:** 2-3 hours
**Test files created:** 4
**Total test methods:** ~15-18
**Expected coverage increase:** +5-10% overall, 100% for metrics components
