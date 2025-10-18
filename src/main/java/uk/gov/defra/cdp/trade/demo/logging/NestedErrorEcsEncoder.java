package uk.gov.defra.cdp.trade.demo.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import co.elastic.logging.JsonUtils;
import co.elastic.logging.logback.EcsEncoder;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

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

    // Pre-compiled regex patterns for performance
    private static final Pattern ERROR_TYPE_PATTERN = Pattern.compile(",?\"error\\.type\":\"(?:[^\"]|\\\\\")*\"");
    private static final Pattern ERROR_MESSAGE_PATTERN = Pattern.compile(",?\"error\\.message\":\"(?:[^\"]|\\\\\")*\"");
    private static final Pattern ERROR_STACK_TRACE_STRING_PATTERN = Pattern.compile(",?\"error\\.stack_trace\":\"(?:[^\"]|\\\\\")*\"");
    private static final Pattern ERROR_STACK_TRACE_ARRAY_PATTERN = Pattern.compile(",?\"error\\.stack_trace\":\\[[^\\]]*\\]");
    private static final Pattern MULTIPLE_COMMAS_PATTERN = Pattern.compile(",+");
    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",}");

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

        // Remove the closing brace and newline
        if (json.endsWith("}\n")) {
            json = json.substring(0, json.length() - 2);
        } else if (json.endsWith("}")) {
            json = json.substring(0, json.length() - 1);
        }

        // Remove flat error.* fields using pre-compiled patterns
        // These patterns handle both presence and absence of fields
        json = ERROR_TYPE_PATTERN.matcher(json).replaceAll("");
        json = ERROR_MESSAGE_PATTERN.matcher(json).replaceAll("");

        // Handle error.stack_trace as both string and array formats
        // String format: "error.stack_trace":"escaped\nstack\ntrace"
        json = ERROR_STACK_TRACE_STRING_PATTERN.matcher(json).replaceAll("");
        // Array format: "error.stack_trace":["line1","line2"]
        json = ERROR_STACK_TRACE_ARRAY_PATTERN.matcher(json).replaceAll("");

        // Remove any trailing commas left by field removal
        json = MULTIPLE_COMMAS_PATTERN.matcher(json).replaceAll(",");
        json = TRAILING_COMMA_PATTERN.matcher(json).replaceAll("}");

        // Build nested error object
        StringBuilder builder = new StringBuilder(json);

        // Add comma before error object if the JSON doesn't end with opening brace or comma
        if (!json.endsWith("{") && !json.endsWith(",")) {
            builder.append(",");
        }

        // Add nested error object
        builder.append("\"error\":{");

        // Extract the actual Throwable if available
        Throwable throwable = extractThrowable(throwableProxy);

        if (throwable != null) {
            // Add type field
            builder.append("\"type\":\"");
            JsonUtils.quoteAsString(throwable.getClass().getName(), builder);
            builder.append("\"");

            // Add message field if present
            String message = throwable.getMessage();
            if (message != null && !message.isEmpty()) {
                builder.append(",\"message\":\"");
                JsonUtils.quoteAsString(message, builder);
                builder.append("\"");
            }

            // Add stack_trace field
            builder.append(",\"stack_trace\":\"");
            JsonUtils.quoteAsString(getStackTraceAsString(throwable), builder);
            builder.append("\"");
        } else {
            // Fallback to proxy information if actual throwable unavailable
            builder.append("\"type\":\"");
            JsonUtils.quoteAsString(throwableProxy.getClassName(), builder);
            builder.append("\"");

            String message = throwableProxy.getMessage();
            if (message != null && !message.isEmpty()) {
                builder.append(",\"message\":\"");
                JsonUtils.quoteAsString(message, builder);
                builder.append("\"");
            }

            // Note: Stack trace from proxy is limited, prefer actual throwable
            builder.append(",\"stack_trace\":\"");
            JsonUtils.quoteAsString("(stack trace unavailable from proxy)", builder);
            builder.append("\"");
        }

        // Close error object
        builder.append("}");

        // Close root JSON object
        builder.append("}\n");

        return builder.toString();
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
