package uk.gov.defra.cdp.trade.demo.example;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Example CRUD operations.
 *
 * Demonstrates CDP compliance:
 * - REST endpoints with proper HTTP methods and status codes
 * - Bean Validation on request bodies
 * - OpenAPI/Swagger documentation
 * - Request tracing via MDC (automatic via RequestTracingFilter)
 * - Structured logging in ECS format
 */
@RestController
@RequestMapping("/example")
@Tag(name = "Example API", description = "CRUD operations for examples (CDP compliance demonstration)")
public class ExampleController {

    private static final Logger logger = LoggerFactory.getLogger(ExampleController.class);

    private final ExampleService exampleService;

    public ExampleController(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    /**
     * Create a new example.
     *
     * @param entity the example to create (validated)
     * @return the created example with generated ID
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create example", description = "Creates a new example with unique name")
    public ExampleEntity create(@Valid @RequestBody ExampleEntity entity) {
        logger.info("POST /example - Creating example with name: {}", entity.getName());
        return exampleService.create(entity);
    }

    /**
     * Get all examples.
     *
     * @return list of all examples
     */
    @GetMapping
    @Operation(summary = "List examples", description = "Returns all examples")
    public List<ExampleEntity> findAll() {
        logger.debug("GET /example - Fetching all examples");
        return exampleService.findAll();
    }

    /**
     * Get an example by ID.
     *
     * @param id the example ID
     * @return the example
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get example by ID", description = "Returns a single example by ID")
    public ExampleEntity findById(@PathVariable String id) {
        logger.debug("GET /example/{} - Fetching example", id);
        return exampleService.findById(id);
    }

    /**
     * Update an existing example.
     *
     * @param id     the example ID
     * @param entity the updated example data (validated)
     * @return the updated example
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update example", description = "Updates an existing example")
    public ExampleEntity update(@PathVariable String id, @Valid @RequestBody ExampleEntity entity) {
        logger.info("PUT /example/{} - Updating example", id);
        return exampleService.update(id, entity);
    }

    /**
     * Delete an example.
     *
     * @param id the example ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete example", description = "Deletes an example by ID")
    public void delete(@PathVariable String id) {
        logger.info("DELETE /example/{} - Deleting example", id);
        exampleService.delete(id);
    }
}
