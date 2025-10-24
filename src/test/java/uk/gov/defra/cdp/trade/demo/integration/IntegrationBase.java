package uk.gov.defra.cdp.trade.demo.integration;

import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
abstract class IntegrationBase {

  static MongoDBContainer MONGO_CONTAINER =
      new MongoDBContainer(DockerImageName.parse("mongo:7.0")).withExposedPorts(27017);

  static {
    Startables.deepStart(MONGO_CONTAINER).join();
  }

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", MONGO_CONTAINER::getReplicaSetUrl);
    registry.add("spring.data.mongodb.ssl.enabled", () -> "false");
  }
}
