package tn.esprit.interfaces;

import java.util.List;

/**
 * Global CRUD contract for all service implementations.
 *
 * @param <T> the entity type managed by this service
 */
public interface GlobalInterface<T> {

    /**
     * Insert a new entity.
     */
    void add(T entity);

    /**
     * Update an existing entity.
     */
    void update(T entity);

    /**
     * Delete an entity by its database ID.
     */
    void delete(int id);

    /**
     * Retrieve all entities.
     */
    List<T> getAll();
}
