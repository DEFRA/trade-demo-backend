package uk.gov.defra.cdp.trade.demo.example;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Example entity demonstrating MongoDB persistence with CDP compliance.
 *
 * This entity showcases:
 * - MongoDB document mapping with @Document
 * - Unique index constraint with @Indexed
 * - Bean Validation with @NotBlank
 * - Automatic ID generation with @Id
 */
@Document(collection = "examples")
public class ExampleEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Value is required")
    private String value;

    private Integer counter;

    private LocalDateTime created;

    // Constructors
    public ExampleEntity() {
    }

    public ExampleEntity(String name, String value) {
        this.name = name;
        this.value = value;
        this.created = LocalDateTime.now();
    }

    // Getters and Setters
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getCounter() {
        return counter;
    }

    public void setCounter(Integer counter) {
        this.counter = counter;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}
