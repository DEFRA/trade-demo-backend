package uk.gov.defra.cdp.trade.demo.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import co.elastic.logging.logback.EcsEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Custom ECS encoder that outputs nested error objects instead of flat error.* fields.
 *
 * This encoder demonstrates the required JSON structure for CDP Data Prepper pipeline
 * compatibility. It transforms the standard logback-ecs-encoder flat field output:
 *
 * <pre>
 * {
 *   "error.type": "java.lang.RuntimeException",
 *   "error.message": "Error message",
 *   "error.stack_trace": "..."
 * }
 * </pre>
 *
 * Into nested object structure compatible with Data Prepper's select_entries processor:
 *
 * <pre>
 * {
 *   "error": {
 *     "type": "java.lang.RuntimeException",
 *     "message": "Error message",
 *     "stack_trace": "..."
 *   }
 * }
 * </pre>
 *
 * This is a proof-of-concept implementation used to demonstrate that nested error
 * objects pass through CDP's Data Prepper select_entries whitelist (error/type,
 * error/message, error/stack_trace) while flat fields (error.type, error.message,
 * error.stack_trace) are filtered out.
 *
 * @see <a href="https://github.com/DEFRA/cdp-tf-modules/blob/main/opensearch_ingestion/vars.tf">CDP Data Prepper whitelist</a>
 */
public class NestedErrorEcsEncoder extends EcsEncoder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public byte[] encode(ILoggingEvent event) {
        // Get the standard ECS encoding from parent
        byte[] parentBytes = super.encode(event);

        // If there's no exception, just return the standard encoding
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            return parentBytes;
        }

        // Transform flat error.* fields to nested error object
        String parentJson = new String(parentBytes, StandardCharsets.UTF_8);
        String transformedJson = transformToNestedError(parentJson, throwableProxy);

        return transformedJson.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Transform flat error.* fields to nested error object.
     *
     * This method:
     * 1. Removes flat error.type, error.message, error.stack_trace fields
     * 2. Adds a nested "error" object with type, message, stack_trace sub-fields
     * 3. Preserves all other ECS fields unchanged
     *
     * Uses Jackson for robust JSON parsing to avoid regex catastrophic backtracking.
     *
     * Package-private for testing.
     *
     * @param json Original JSON from parent EcsEncoder
     * @param throwableProxy Exception information
     * @return Transformed JSON with nested error object
     */
    String transformToNestedError(String json, IThrowableProxy throwableProxy) {
        // Defensive null checks
        if (json == null || json.isEmpty()) {
            return "{}\n";
        }
        if (throwableProxy == null) {
            return json;
        }

        try {
            // Parse JSON using Jackson
            json = json.trim();
            ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(json);

            // Remove flat error.* fields
            root.remove("error.type");
            root.remove("error.message");
            root.remove("error.stack_trace");

            // Create nested error object
            ObjectNode errorNode = OBJECT_MAPPER.createObjectNode();

            // Extract the actual Throwable if available
            Throwable throwable = extractThrowable(throwableProxy);

            if (throwable != null) {
                // Add type field
                errorNode.put("type", throwable.getClass().getName());

                // Add message field if present
                String message = throwable.getMessage();
                if (message != null && !message.isEmpty()) {
                    errorNode.put("message", message);
                }

                // Add stack_trace field
                errorNode.put("stack_trace", getStackTraceAsString(throwable));
            } else {
                // Fallback to proxy information if actual throwable unavailable
                errorNode.put("type", throwableProxy.getClassName());

                String message = throwableProxy.getMessage();
                if (message != null && !message.isEmpty()) {
                    errorNode.put("message", message);
                }

                // Note: Stack trace from proxy is limited, prefer actual throwable
                errorNode.put("stack_trace", "(stack trace unavailable from proxy)");
            }

            // Add nested error object to root
            root.set("error", errorNode);

            // Serialize back to JSON with newline
            return OBJECT_MAPPER.writeValueAsString(root) + "\n";

        } catch (Exception e) {
            // If JSON parsing fails, return original JSON
            // This is a fallback to ensure logging doesn't break
            return json.endsWith("\n") ? json : json + "\n";
        }
    }

    /**
     * Extract the actual Throwable from the proxy if available.
     *
     * Package-private for testing.
     *
     * @param throwableProxy Logback's throwable proxy
     * @return The actual Throwable instance, or null if unavailable
     */
    Throwable extractThrowable(IThrowableProxy throwableProxy) {
        if (throwableProxy instanceof ThrowableProxy) {
            return ((ThrowableProxy) throwableProxy).getThrowable();
        }
        return null;
    }

    /**
     * Convert throwable to a formatted stack trace string.
     *
     * This matches the format produced by the standard ECS encoder for consistency.
     *
     * @param throwable The exception to format
     * @return Formatted stack trace as a single string
     */
    private String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
        }
        return sw.toString();
    }
}
