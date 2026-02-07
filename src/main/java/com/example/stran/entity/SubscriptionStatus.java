package com.example.stran.entity;

/**
 * Subscription status enum — mirrors stran-subscription-service's SubscriptionStatus.
 * Used for read-only filtering of active subscriptions.
 */
public enum SubscriptionStatus {
    ACTIVE,
    PAUSED,
    CANCELLED,
    EXPIRED,
    NOTIFIED
}
