package com.example.stran.dto.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Notification message published to the notifications MSK topic
 * when a subscription matches available inventory.
 *
 * <p>Consumed downstream by stran-notification-service for guest delivery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationMessage {

    /** Unique notification identifier */
    private String notificationId;

    /** Matched subscription ID */
    private Long subscriptionId;

    /** Guest identifier */
    private String guestId;

    /** Hotel property code (e.g., "FNLCO") */
    private String propCode;

    /** Check-in date the guest subscribed for (e.g., "2026-03-09") */
    private String checkInDate;

    /** Number of nights the guest wants */
    private Integer nights;

    /** Human-readable notification message */
    private String message;

    /** Notification creation timestamp */
    private Instant timestamp;
}
