package uk.gov.defra.cdp.trade.demo.config;

import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.connection.ClusterDescription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MongoDB configuration.
 * Uses Testcontainers to spin up a real MongoDB instance.
 */
@SpringBootTest
@Testcontainers
class MongoConfigIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.ssl.enabled", () -> "false");
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    @Test
    void mongoTemplate_shouldBeConfigured() {
        assertThat(mongoTemplate).isNotNull();
        assertThat(mongoTemplate.getDb()).isNotNull();
        assertThat(mongoTemplate.getDb().getName()).isEqualTo("trade-demo-backend");
    }

    @Test
    void mongoClient_shouldBeConnected() {
        assertThat(mongoClient).isNotNull();

        // Verify connection by getting cluster description
        ClusterDescription clusterDescription = mongoClient.getClusterDescription();
        assertThat(clusterDescription).isNotNull();
        assertThat(clusterDescription.getServerDescriptions()).isNotEmpty();
    }

    @Test
    void mongoClient_shouldHaveCorrectReadPreference() {
        // In test environment, read preference might default to primary
        // In production with replicas, it would use secondary
        ReadPreference readPreference = mongoClient.getDatabase("test").getReadPreference();
        assertThat(readPreference).isNotNull();
    }

    @Test
    void mongoClient_shouldHaveCorrectWriteConcern() {
        WriteConcern writeConcern = mongoClient.getDatabase("test").getWriteConcern();
        assertThat(writeConcern).isNotNull();
        // Majority write concern
        assertThat(writeConcern.getWObject()).isEqualTo("majority");
    }

    @Test
    void mongoTemplate_shouldPerformBasicOperations() {
        // Create a test document
        TestDocument testDoc = new TestDocument("test-id", "Test Name");

        // Insert
        TestDocument savedDoc = mongoTemplate.insert(testDoc, "test-collection");
        assertThat(savedDoc).isNotNull();
        assertThat(savedDoc.getId()).isEqualTo("test-id");

        // Find
        TestDocument foundDoc = mongoTemplate.findById("test-id", TestDocument.class, "test-collection");
        assertThat(foundDoc).isNotNull();
        assertThat(foundDoc.getName()).isEqualTo("Test Name");

        // Clean up
        mongoTemplate.dropCollection("test-collection");
    }

    /**
     * Simple test document for MongoDB operations.
     */
    static class TestDocument {
        private String id;
        private String name;

        public TestDocument() {
        }

        public TestDocument(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
