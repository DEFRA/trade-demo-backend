package uk.gov.defra.cdp.trade.demo.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import uk.gov.defra.cdp.trade.demo.common.metrics.MetricsService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for Example CRUD operations.
 *
 * Demonstrates CDP compliance:
 * - Structured logging with SLF4J (ECS format)
 * - Custom CloudWatch metrics for business events
 * - Proper exception handling with meaningful messages
 * - MongoDB operations with Spring Data
 */
@Service
public class ExampleService {

    private static final Logger logger = LoggerFactory.getLogger(ExampleService.class);

    private final ExampleRepository repository;
    private final MetricsService metricsService;

    public ExampleService(ExampleRepository repository, MetricsService metricsService) {
        this.repository = repository;
        this.metricsService = metricsService;
    }

    /**
     * Create a new example.
     *
     * @param entity the example to create
     * @return the created example with generated ID
     * @throws ConflictException if an example with the same name already exists
     */
    public ExampleEntity create(ExampleEntity entity) {
        logger.info("Creating example with name: {}", entity.getName());

        // Check for duplicate name
        if (repository.findByName(entity.getName()).isPresent()) {
            logger.warn("Example with name '{}' already exists", entity.getName());
            throw new ConflictException("Example with name '" + entity.getName() + "' already exists");
        }

        entity.setCreated(LocalDateTime.now());

        try {
            ExampleEntity saved = repository.save(entity);
            metricsService.counter("example_created");
            logger.info("Created example with id: {}", saved.getId());
            return saved;

        } catch (DuplicateKeyException e) {
            // Handle race condition where two requests create same name simultaneously
            logger.warn("Duplicate key error creating example: {}", entity.getName());
            throw new ConflictException("Example with name '" + entity.getName() + "' already exists");
        }
    }

    /**
     * Get all examples.
     *
     * @return list of all examples
     */
    public List<ExampleEntity> findAll() {
        logger.debug("Fetching all examples");
        List<ExampleEntity> examples = repository.findAll();
        logger.debug("Found {} examples", examples.size());
        return examples;
    }

    /**
     * Get an example by ID.
     *
     * @param id the example ID
     * @return the example
     * @throws NotFoundException if the example does not exist
     */
    public ExampleEntity findById(String id) {
        logger.debug("Fetching example with id: {}", id);
        return repository.findById(id)
            .orElseThrow(() -> {
                logger.warn("Example not found with id: {}", id);
                return new NotFoundException("Example not found with id: " + id);
            });
    }

    /**
     * Update an existing example.
     *
     * @param id     the example ID
     * @param entity the updated example data
     * @return the updated example
     * @throws NotFoundException if the example does not exist
     * @throws ConflictException if the new name conflicts with another example
     */
    public ExampleEntity update(String id, ExampleEntity entity) {
        logger.info("Updating example with id: {}", id);

        // Check if example exists
        ExampleEntity existing = findById(id);

        // Check for name conflict with other examples
        if (!existing.getName().equals(entity.getName())) {
            repository.findByName(entity.getName()).ifPresent(conflict -> {
                if (!conflict.getId().equals(id)) {
                    logger.warn("Example with name '{}' already exists", entity.getName());
                    throw new ConflictException("Example with name '" + entity.getName() + "' already exists");
                }
            });
        }

        // Update fields
        existing.setName(entity.getName());
        existing.setValue(entity.getValue());
        existing.setCounter(entity.getCounter());

        ExampleEntity updated = repository.save(existing);
        metricsService.counter("example_updated");
        logger.info("Updated example with id: {}", updated.getId());
        return updated;
    }

    /**
     * Delete an example.
     *
     * @param id the example ID
     * @throws NotFoundException if the example does not exist
     */
    public void delete(String id) {
        logger.info("Deleting example with id: {}", id);

        // Check if example exists
        findById(id);

        repository.deleteById(id);
        metricsService.counter("example_deleted");
        logger.info("Deleted example with id: {}", id);
    }
}
