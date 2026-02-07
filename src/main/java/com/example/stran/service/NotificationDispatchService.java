package com.example.stran.service;

import com.example.stran.dto.inventory.InventoryEventBody;
import com.example.stran.dto.notification.NotificationMessage;
import com.example.stran.entity.StaySubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Builds {@link NotificationMessage} instances for matched subscriptions
 * and delegates publishing to the notification Kafka producer.
 *
 * <p>Called after the matching service identifies subscriptions whose
 * criteria align with an incoming inventory event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationProducer notificationProducer;

    /**
     * Build and dispatch notification messages for every matched subscription.
     *
     * @param matchedSubscriptions subscriptions that match the inventory event
     * @param eventBody            the inventory event that triggered the match
     * @return the list of built notification messages (useful for logging / testing)
     */
    public List<NotificationMessage> dispatch(List<StaySubscription> matchedSubscriptions,
                                              InventoryEventBody eventBody) {
        if (matchedSubscriptions == null || matchedSubscriptions.isEmpty()) {
            log.debug("No matched subscriptions to dispatch for propCode={}", eventBody.getPropCode());
            return List.of();
        }

        List<NotificationMessage> messages = matchedSubscriptions.stream()
                .map(sub -> buildNotification(sub, eventBody))
                .toList();

        notificationProducer.send(messages);

        log.info("Dispatched {} notification(s) for propCode={} on date={}",
                messages.size(), eventBody.getPropCode(), eventBody.getStartDate());

        return messages;
    }

    /**
     * Build a single {@link NotificationMessage} from a matched subscription
     * and the triggering inventory event.
     */
    private NotificationMessage buildNotification(StaySubscription subscription,
                                                  InventoryEventBody eventBody) {
        int nights = subscription.getNights();

        NotificationMessage notification = NotificationMessage.builder()
                .notificationId(UUID.randomUUID().toString())
                .subscriptionId(subscription.getId())
                .guestId(subscription.getGuestId())
                .propCode(eventBody.getPropCode())
                .checkInDate(subscription.getCheckInDate().toString())
                .nights(nights)
                .message(String.format(
                        "Availability found at property %s for %d night(s) starting %s",
                        eventBody.getPropCode(), nights, subscription.getCheckInDate()))
                .timestamp(Instant.now())
                .build();

        log.debug("Built notification id={} for subscriptionId={}, guestId={}",
                notification.getNotificationId(), subscription.getId(), subscription.getGuestId());

        return notification;
    }
}
