package com.example.stran.repository;

import com.example.stran.entity.Property;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Read-only repository for looking up properties.
 * Used to resolve propCode (from MSK inventory events) to propertyId.
 *
 * <p>Extends {@link Repository} (not JpaRepository) to expose only read methods,
 * enforcing that stran never writes to the property table.
 */
@Transactional(readOnly = true)
public interface PropertyRepository extends Repository<Property, Long> {

    /**
     * Find a property by its property code (e.g., "FNLCO").
     *
     * @param propCode the property code from the MSK event
     * @return the property if found
     */
    Optional<Property> findByPropCode(String propCode);
}
