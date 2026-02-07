package com.example.stran.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Read-only entity mapping to the hmstst.property table.
 * Used to resolve propCode (from MSK events) to propertyId (used in subscriptions).
 *
 * <p>This table is owned by the subscription-service; stran only reads from it.
 */
@Entity
@Table(name = "property", schema = "hmstst")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Property {

    @Id
    @Column(name = "property_id")
    private Long propertyId;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    /** ctyhocn = propCode + brand (e.g., FNLCOHF = FNLCO + HF) */
    @Column(name = "ctyhocn", nullable = false, length = 64, unique = true)
    private String ctyhocn;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "brand", length = 10)
    private String brand;

    /** Property code used in MSK events (e.g., "FNLCO") */
    @Column(name = "prop_code", length = 16)
    private String propCode;

    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @Column(name = "updated_date")
    private Instant updatedDate;

    @Version
    @Column(name = "persistence_version")
    private Integer persistenceVersion;
}
