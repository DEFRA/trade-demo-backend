package uk.gov.defra.cdp.trade.demo.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.connection.ConnectionPoolSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import jakarta.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB configuration for CDP Java Backend Template.
 *
 * Configures MongoDB connection with:
 * - AWS IAM authentication (via connection string authMechanism=MONGODB-AWS)
 * - Custom SSL/TLS certificates from TRUSTSTORE_* environment variables
 * - Read preference: secondary (configurable)
 * - Write concern: majority (configurable)
 * - Connection pooling
 * - Graceful shutdown
 *
 * Connection string format for AWS IAM auth:
 * mongodb://host:port/database?authMechanism=MONGODB-AWS&authSource=$external
 */
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);
    private final SSLContext customSslContext;
    private MongoClient mongoClientInstance;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Value("${spring.data.mongodb.read-preference:secondary}")
    private String readPreference;

    @Value("${spring.data.mongodb.write-concern:majority}")
    private String writeConcern;

    @Value("${spring.data.mongodb.connection-pool.min-size:10}")
    private int connectionPoolMinSize;

    @Value("${spring.data.mongodb.connection-pool.max-size:100}")
    private int connectionPoolMaxSize;

    @Value("${spring.data.mongodb.connection-pool.max-wait-time-ms:2000}")
    private long connectionPoolMaxWaitTimeMs;

    @Value("${spring.data.mongodb.connection-pool.max-connection-idle-time-ms:60000}")
    private long connectionPoolMaxConnectionIdleTimeMs;

    @Value("${spring.data.mongodb.ssl.enabled:true}")
    private boolean sslEnabled;

    public MongoConfig(SSLContext customSslContext) {
        this.customSslContext = customSslContext;
    }

    /**
     * Overrides mongoClient() to explicitly register it as a Spring bean.
     *
     * Note: AbstractMongoClientConfiguration.mongoClient() is NOT annotated with @Bean,
     * so it doesn't register the MongoClient in the ApplicationContext by default.
     * This override adds @Bean annotation to make MongoClient available for autowiring.
     *
     * @return MongoClient instance configured with settings from configureClientSettings()
     */
    @Override
    @Bean
    public MongoClient mongoClient() {
        this.mongoClientInstance = super.mongoClient();
        return this.mongoClientInstance;
    }

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        logger.info("Configuring MongoDB client settings");

        // Parse connection string (includes host, port, authentication, etc.)
        ConnectionString connectionString = new ConnectionString(mongoUri);
        builder.applyConnectionString(connectionString);

        // Configure read preference
        ReadPreference readPref = parseReadPreference(readPreference);
        builder.readPreference(readPref);
        logger.info("MongoDB read preference: {}", readPref);

        // Configure write concern
        WriteConcern writeCon = parseWriteConcern(writeConcern);
        builder.writeConcern(writeCon);
        logger.info("MongoDB write concern: {}", writeCon);

        // Configure connection pool settings
        builder.applyToConnectionPoolSettings(settings ->
            configureConnectionPool(settings)
        );

        // Configure SSL with custom certificates (only if enabled)
        if (sslEnabled) {
            builder.applyToSslSettings(sslSettings -> {
                sslSettings.enabled(true);
                sslSettings.context(customSslContext);
                logger.info("MongoDB SSL configured with custom trust store");
            });
        } else {
            logger.info("MongoDB SSL disabled (typically for local/test environments)");
        }

        logger.info("MongoDB client configuration complete");
    }

    private void configureConnectionPool(ConnectionPoolSettings.Builder settings) {
        settings
            .minSize(connectionPoolMinSize)
            .maxSize(connectionPoolMaxSize)
            .maxWaitTime(connectionPoolMaxWaitTimeMs, TimeUnit.MILLISECONDS)
            .maxConnectionIdleTime(connectionPoolMaxConnectionIdleTimeMs, TimeUnit.MILLISECONDS);

        logger.info("MongoDB connection pool: min={}, max={}, maxWaitTime={}ms, maxIdleTime={}ms",
                connectionPoolMinSize, connectionPoolMaxSize, connectionPoolMaxWaitTimeMs,
                connectionPoolMaxConnectionIdleTimeMs);
    }

    private ReadPreference parseReadPreference(String preference) {
        return switch (preference.toLowerCase()) {
            case "primary" -> ReadPreference.primary();
            case "primarypreferred" -> ReadPreference.primaryPreferred();
            case "secondary" -> ReadPreference.secondary();
            case "secondarypreferred" -> ReadPreference.secondaryPreferred();
            case "nearest" -> ReadPreference.nearest();
            default -> {
                logger.warn("Unknown read preference '{}', defaulting to 'secondary'", preference);
                yield ReadPreference.secondary();
            }
        };
    }

    private WriteConcern parseWriteConcern(String concern) {
        return switch (concern.toLowerCase()) {
            case "acknowledged" -> WriteConcern.ACKNOWLEDGED;
            case "w1" -> WriteConcern.W1;
            case "w2" -> WriteConcern.W2;
            case "w3" -> WriteConcern.W3;
            case "majority" -> WriteConcern.MAJORITY;
            case "unacknowledged" -> WriteConcern.UNACKNOWLEDGED;
            case "journaled" -> WriteConcern.JOURNALED;
            default -> {
                logger.warn("Unknown write concern '{}', defaulting to 'MAJORITY'", concern);
                yield WriteConcern.MAJORITY;
            }
        };
    }

    @PreDestroy
    public void destroy() {
        if (mongoClientInstance != null) {
            logger.info("Shutting down MongoDB client");
            try {
                mongoClientInstance.close();
                logger.info("MongoDB client closed successfully");
            } catch (Exception e) {
                logger.error("Error closing MongoDB client", e);
            }
        }
    }
}
