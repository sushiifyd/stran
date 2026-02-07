package com.example.stran.entity;

/**
 * Subscription search type enum — mirrors stran-subscription-service's SubscriptionSearchType.
 *
 * <p>MVP: Only PROPERTY type is matched. AREA matching is Phase 2.
 */
public enum SubscriptionSearchType {
    /** Subscription for a specific property */
    PROPERTY,

    /** Subscription for properties in a geographic area (Phase 2) */
    AREA
}
