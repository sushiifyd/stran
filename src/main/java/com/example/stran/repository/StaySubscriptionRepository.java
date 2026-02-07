package com.example.stran.repository;

import com.example.stran.entity.StaySubscription;
import com.example.stran.entity.SubscriptionSearchType;
import com.example.stran.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only repository for querying guest subscriptions.
 * Used to find active subscriptions that match incoming inventory events.
 */
@Repository
public interface StaySubscriptionRepository extends JpaRepository<StaySubscription, Long> {

    /**
     * Find subscriptions matching a specific property and check-in date,
     * filtered by the provided status and search type.
     *
     * <p>Used by the availability matching service to find subscriptions that
     * could match an incoming inventory event for a given property and date.
     * The caller is responsible for verifying the length-of-stay pattern.
     *
     * @param propertyId the property ID (resolved from propCode via PropertyRepository)
     * @param checkInDate the date from the inventory event
     * @param status the subscription status to filter by (e.g., ACTIVE)
     * @param searchType the search type to filter by (e.g., PROPERTY)
     * @return list of matching subscriptions
     */
    @Query("SELECT s FROM StaySubscription s " +
           "WHERE s.propertyId = :propertyId " +
           "AND s.checkInDate = :checkInDate " +
           "AND s.status = :status " +
           "AND s.searchType = :searchType")
    List<StaySubscription> findMatchingSubscriptions(
            @Param("propertyId") Long propertyId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("status") SubscriptionStatus status,
            @Param("searchType") SubscriptionSearchType searchType);

    /**
     * Convenience method: find active PROPERTY subscriptions for a property and check-in date.
     */
    default List<StaySubscription> findActivePropertySubscriptions(Long propertyId, LocalDate checkInDate) {
        return findMatchingSubscriptions(propertyId, checkInDate, SubscriptionStatus.ACTIVE, SubscriptionSearchType.PROPERTY);
    }
}
