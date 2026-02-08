package com.example.stran.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Read-only entity mapping to the stay_subscriptions table.
 * Mirrors stran-subscription-service's StaySubscription entity.
 *
 * <p>This table is owned by the subscription-service; stran only reads from it
 * to match incoming inventory events against active guest subscriptions.
 */
@Entity
@Table(name = "stay_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class StaySubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guest_id", nullable = false, length = 64)
    private String guestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", nullable = false, length = 20)
    private SubscriptionSearchType searchType;

    /** Property ID — used for PROPERTY-type subscription matching */
    @Column(name = "property_id")
    private Long propertyId;

    /* ---- Area search fields (Phase 2) ---- */

    @Column(name = "search_area", length = 100)
    private String searchArea;

    @Column(name = "search_country_code", length = 2)
    private String searchCountryCode;

    @Column(name = "search_radius_km")
    private Integer searchRadiusKm;

    @Column(name = "search_latitude")
    private Double searchLatitude;

    @Column(name = "search_longitude")
    private Double searchLongitude;

    /* ---- Stay criteria ---- */

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "max_price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxPricePerNight;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "num_adults", nullable = false)
    private Integer numAdults;

    @Column(name = "num_children")
    private Integer numChildren;

    @Column(name = "num_rooms", nullable = false)
    private Integer numRooms;

    /* ---- Status & timestamps ---- */

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "check_count", nullable = false)
    private Integer checkCount;

    /* ---- Convenience methods ---- */

    /**
     * Calculate the number of nights from check-in to check-out.
     */
    public int getNights() {
        return (int) ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean isPropertySubscription() {
        return searchType == SubscriptionSearchType.PROPERTY && propertyId != null;
    }

    public boolean isAreaSubscription() {
        return searchType == SubscriptionSearchType.AREA;
    }
}
