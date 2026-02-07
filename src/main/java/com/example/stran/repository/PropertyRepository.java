package com.example.stran.repository;

import com.example.stran.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Read-only repository for looking up properties.
 * Used to resolve propCode (from MSK inventory events) to propertyId.
 */
@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    /**
     * Find a property by its property code (e.g., "FNLCO").
     *
     * @param propCode the property code from the MSK event
     * @return the property if found
     */
    Optional<Property> findByPropCode(String propCode);
}
