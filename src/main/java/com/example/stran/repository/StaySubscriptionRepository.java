package com.example.stran.repository;

import com.example.stran.entity.StaySubscription;
import com.example.stran.entity.SubscriptionSearchType;
import com.example.stran.entity.SubscriptionStatus;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only repository for querying guest subscriptions.
 * Used to find active subscriptions that match incoming inventory events.
 *
 * <p>Extends {@link Repository} (not JpaRepository) to expose only read methods,
 * enforcing that stran never writes to the stay_subscriptions table.
 */
@Transactional(readOnly = true)
public interface StaySubscriptionRepository extends Repository<StaySubscription, Long> {

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
    List<StaySubscription> findByPropertyIdAndCheckInDateAndStatusAndSearchType(
            Long propertyId,
            LocalDate checkInDate,
            SubscriptionStatus status,
            SubscriptionSearchType searchType);

    /**
     * Convenience method: find active PROPERTY subscriptions for a property and check-in date.
     */
    default List<StaySubscription> findActivePropertySubscriptions(Long propertyId, LocalDate checkInDate) {
        return findByPropertyIdAndCheckInDateAndStatusAndSearchType(
                propertyId, checkInDate, SubscriptionStatus.ACTIVE, SubscriptionSearchType.PROPERTY);
    }
}
