package com.marketplace.interfaces;

import java.util.List;

/**
 * IService<T> — Generic CRUD interface.
 *
 * Every service class implements this interface for its own model.
 * Methods:
 *   add    → INSERT
 *   update → UPDATE
 *   delete → DELETE
 *   getAll → SELECT *
 */
public interface IService<T> {

    /** Insert a new record into the database. */
    void add(T t);

    /** Update an existing record in the database. */
    void update(T t);

    /** Delete a record by its ID. */
    void delete(int id);

    /** Retrieve all records from the database. */
    List<T> getAll();
}
