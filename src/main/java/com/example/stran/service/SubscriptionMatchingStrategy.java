package com.example.stran.service;

import com.example.stran.dto.inventory.InventoryEventBody;
import com.example.stran.entity.StaySubscription;

import java.util.List;

/**
 * Strategy interface for matching inventory events against subscriptions.
 *
 * <p>Implementations handle different subscription types (PROPERTY, AREA).
 * This allows adding new matching strategies (e.g., area-based search in Phase 2)
 * without modifying existing code.
 */
public interface SubscriptionMatchingStrategy {

    /**
     * Find subscriptions that match the given inventory event.
     *
     * @param eventBody the inventory event body containing availability data
     * @return list of subscriptions that match the event
     */
    List<StaySubscription> findMatchingSubscriptions(InventoryEventBody eventBody);

    /**
     * Whether this strategy supports the given event.
     * Used to select the appropriate strategy at runtime.
     *
     * @param eventBody the inventory event body
     * @return true if this strategy can handle the event
     */
    boolean supports(InventoryEventBody eventBody);
}
