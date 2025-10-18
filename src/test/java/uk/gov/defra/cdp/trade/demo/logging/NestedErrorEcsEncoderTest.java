package uk.gov.defra.cdp.trade.demo.logging;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NestedErrorEcsEncoder.
 *
 * Tests the transformation logic that converts flat error.* fields to nested error objects.
 */
class NestedErrorEcsEncoderTest {

    private NestedErrorEcsEncoder encoder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        encoder = new NestedErrorEcsEncoder();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldTransformFlatErrorFieldsToNestedObject() throws Exception {
        // Given: JSON with flat error.* fields (from standard EcsEncoder)
        String inputJson = """
            {"@timestamp":"2025-10-18T12:00:00.000Z","log.level":"ERROR","message":"Test error","error.type":"java.lang.RuntimeException","error.message":"Test exception","error.stack_trace":"java.lang.RuntimeException: Test exception\\n\\tat TestClass.method(TestClass.java:10)"}
            """;

        RuntimeException exception = new RuntimeException("Test exception");
        ThrowableProxy proxy = new ThrowableProxy(exception);

        // When: Transforming to nested format
        String result = encoder.transformToNestedError(inputJson, proxy);

        // Then: Should produce valid JSON
        JsonNode json = objectMapper.readTree(result);

        // And: Should have nested error object
        assertTrue(json.has("error"), "Should have nested error object");
        assertTrue(json.get("error").isObject(), "error should be an object");

        // And: Should NOT have flat error.* fields
        assertFalse(json.has("error.type"), "Should not have flat error.type");
        assertFalse(json.has("error.message"), "Should not have flat error.message");
        assertFalse(json.has("error.stack_trace"), "Should not have flat error.stack_trace");

        // And: Nested error should have correct structure
        JsonNode error = json.get("error");
        assertTrue(error.has("type"), "error should have type");
        assertTrue(error.has("message"), "error should have message");
        assertTrue(error.has("stack_trace"), "error should have stack_trace");

        // And: Should preserve other fields
        assertEquals("ERROR", json.get("log.level").asText());
        assertEquals("Test error", json.get("message").asText());
    }

    @Test
    void shouldHandleExceptionWithNullMessage() throws Exception {
        // Given: Exception with null message
        String inputJson = """
            {"@timestamp":"2025-10-18T12:00:00.000Z","log.level":"ERROR","message":"Log message","error.type":"java.lang.RuntimeException","error.stack_trace":"java.lang.RuntimeException\\n\\tat Test.method(Test.java:1)"}
            """;

        RuntimeException exception = new RuntimeException((String) null);
        ThrowableProxy proxy = new ThrowableProxy(exception);

        // When: Transforming
        String result = encoder.transformToNestedError(inputJson, proxy);

        // Then: Should produce valid JSON
        JsonNode json = objectMapper.readTree(result);
        JsonNode error = json.get("error");

        // And: Should have type and stack_trace
        assertTrue(error.has("type"));
        assertTrue(error.has("stack_trace"));
        assertEquals("java.lang.RuntimeException", error.get("type").asText());
    }

    @Test
    void shouldHandleNestedExceptions() throws Exception {
        // Given: Nested exception
        String inputJson = """
            {"@timestamp":"2025-10-18T12:00:00.000Z","log.level":"ERROR","message":"Nested error","error.type":"java.lang.IllegalStateException","error.message":"Outer","error.stack_trace":"java.lang.IllegalStateException: Outer\\n\\tat Test.java:1\\nCaused by: java.lang.NullPointerException: Inner\\n\\tat Test.java:2"}
            """;

        NullPointerException cause = new NullPointerException("Inner");
        IllegalStateException exception = new IllegalStateException("Outer", cause);
        ThrowableProxy proxy = new ThrowableProxy(exception);

        // When: Transforming
        String result = encoder.transformToNestedError(inputJson, proxy);

        // Then: Should produce valid JSON with nested error
        JsonNode json = objectMapper.readTree(result);
        JsonNode error = json.get("error");

        assertTrue(error.has("type"));
        assertTrue(error.has("message"));
        assertTrue(error.has("stack_trace"));

        // And: Stack trace should include cause
        String stackTrace = error.get("stack_trace").asText();
        assertTrue(stackTrace.contains("IllegalStateException"));
        assertTrue(stackTrace.contains("Caused by"));
        assertTrue(stackTrace.contains("NullPointerException"));
    }

    @Test
    void shouldPreserveAllNonErrorFields() throws Exception {
        // Given: JSON with multiple ECS fields
        String inputJson = """
            {"@timestamp":"2025-10-18T12:00:00.000Z","log.level":"ERROR","message":"Test","service.name":"test-service","service.version":"1.0.0","trace.id":"abc123","error.type":"java.lang.RuntimeException","error.message":"Error","error.stack_trace":"java.lang.RuntimeException: Error"}
            """;

        RuntimeException exception = new RuntimeException("Error");
        ThrowableProxy proxy = new ThrowableProxy(exception);

        // When: Transforming
        String result = encoder.transformToNestedError(inputJson, proxy);

        // Then: All non-error fields should be preserved
        JsonNode json = objectMapper.readTree(result);

        assertEquals("2025-10-18T12:00:00.000Z", json.get("@timestamp").asText());
        assertEquals("ERROR", json.get("log.level").asText());
        assertEquals("Test", json.get("message").asText());
        assertEquals("test-service", json.get("service.name").asText());
        assertEquals("1.0.0", json.get("service.version").asText());
        assertEquals("abc123", json.get("trace.id").asText());
    }

    @Test
    void shouldProduceValidJsonWithoutTrailingCommas() throws Exception {
        // Given: Input JSON
        String inputJson = """
            {"@timestamp":"2025-10-18T12:00:00.000Z","log.level":"ERROR","message":"Test","error.type":"java.lang.RuntimeException","error.message":"Error","error.stack_trace":"stack"}
            """;

        RuntimeException exception = new RuntimeException("Error");
        ThrowableProxy proxy = new ThrowableProxy(exception);

        // When: Transforming
        String result = encoder.transformToNestedError(inputJson, proxy);

        // Then: Should not have malformed JSON
        assertFalse(result.contains(",,"), "Should not have double commas");
        assertFalse(result.contains(",}"), "Should not have trailing comma before closing brace");
        assertFalse(result.contains("{,"), "Should not have leading comma after opening brace");

        // And: Should parse as valid JSON
        assertDoesNotThrow(() -> objectMapper.readTree(result), "Should be valid JSON");
    }

    @Test
    void shouldHandleJsonWithNewlineEnding() throws Exception {
        // Given: JSON ending with newline (typical from EcsEncoder)
        String inputJson = "{\"@timestamp\":\"2025-10-18T12:00:00.000Z\",\"log.level\":\"ERROR\",\"message\":\"Test\",\"error.type\":\"java.lang.RuntimeException\",\"error.message\":\"Error\",\"error.stack_trace\":\"stack\"}\n";

        RuntimeException exception = new RuntimeException("Error");
        ThrowableProxy proxy = new ThrowableProxy(exception);

        // When: Transforming
        String result = encoder.transformToNestedError(inputJson, proxy);

        // Then: Should produce valid JSON ending with newline
        assertTrue(result.endsWith("}\n"), "Should end with closing brace and newline");

        // And: Should parse correctly
        assertDoesNotThrow(() -> objectMapper.readTree(result));
    }

    @Test
    void shouldHandleJsonWithoutNewlineEnding() throws Exception {
        // Given: JSON without newline ending
        String inputJson = "{\"@timestamp\":\"2025-10-18T12:00:00.000Z\",\"log.level\":\"ERROR\",\"message\":\"Test\",\"error.type\":\"java.lang.RuntimeException\",\"error.message\":\"Error\",\"error.stack_trace\":\"stack\"}";

        RuntimeException exception = new RuntimeException("Error");
        ThrowableProxy proxy = new ThrowableProxy(exception);

        // When: Transforming
        String result = encoder.transformToNestedError(inputJson, proxy);

        // Then: Should produce valid JSON
        assertTrue(result.endsWith("}\n"), "Should end with closing brace and newline");
        assertDoesNotThrow(() -> objectMapper.readTree(result));
    }

    @Test
    void shouldExtractThrowableFromProxy() {
        // Given: A ThrowableProxy with actual exception
        RuntimeException exception = new RuntimeException("Test");
        ThrowableProxy proxy = new ThrowableProxy(exception);

        // When: Extracting throwable
        Throwable extracted = encoder.extractThrowable(proxy);

        // Then: Should return the actual exception
        assertNotNull(extracted);
        assertSame(exception, extracted);
        assertEquals("Test", extracted.getMessage());
    }

    @Test
    void shouldReturnNullForNonThrowableProxy() {
        // Given: A mock IThrowableProxy that's not a ThrowableProxy
        IThrowableProxy mockProxy = new IThrowableProxy() {
            @Override
            public String getMessage() {
                return "mock";
            }

            @Override
            public String getClassName() {
                return "MockException";
            }

            @Override
            public StackTraceElementProxy[] getStackTraceElementProxyArray() {
                return new StackTraceElementProxy[0];
            }

            @Override
            public int getCommonFrames() {
                return 0;
            }

            @Override
            public IThrowableProxy getCause() {
                return null;
            }

            @Override
            public IThrowableProxy[] getSuppressed() {
                return new IThrowableProxy[0];
            }

            @Override
            public boolean isCyclic() {
                return false;
            }
        };

        // When: Extracting throwable
        Throwable extracted = encoder.extractThrowable(mockProxy);

        // Then: Should return null
        assertNull(extracted);
    }

}
