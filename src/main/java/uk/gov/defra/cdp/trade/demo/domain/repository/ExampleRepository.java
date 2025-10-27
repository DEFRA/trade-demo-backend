package uk.gov.defra.cdp.trade.demo.domain.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import uk.gov.defra.cdp.trade.demo.domain.Example;

/**
 * Spring Data MongoDB repository for ExampleEntity.
 *
 * Provides standard CRUD operations plus custom query methods.
 * Spring Data MongoDB automatically implements this interface.
 */
@Repository
public interface ExampleRepository extends MongoRepository<Example, String> {

    /**
     * Find an example by name.
     * Used to enforce unique name constraint at application level.
     *
     * @param name the name to search for
     * @return Optional containing the entity if found
     */
    Optional<Example> findByName(String name);
}
