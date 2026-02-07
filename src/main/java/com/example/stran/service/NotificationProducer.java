package com.example.stran.service;

import com.example.stran.dto.notification.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publishes {@link NotificationMessage} instances to the notifications MSK topic.
 *
 * <p>Uses the subscription ID as the Kafka message key so that all notifications
 * for the same subscription land on the same partition, preserving ordering
 * per subscription.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationMessage> notificationKafkaTemplate;

    @Value("${kafka.topic.notifications}")
    private final String notificationsTopic;

    /**
     * Publish a list of notification messages to the notifications topic.
     *
     * @param messages the notification messages to publish
     */
    public void send(List<NotificationMessage> messages) {
        messages.forEach(this::sendSingle);
    }

    /**
     * Publish a single notification message.
     *
     * @param message the notification message to publish
     */
    public void sendSingle(NotificationMessage message) {
        String key = String.valueOf(message.getSubscriptionId());

        notificationKafkaTemplate.send(notificationsTopic, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish notification id={} for subscriptionId={}: {}",
                                message.getNotificationId(), message.getSubscriptionId(), ex.getMessage(), ex);
                    } else {
                        log.info("Published notification id={} for subscriptionId={} to topic={} partition={} offset={}",
                                message.getNotificationId(),
                                message.getSubscriptionId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
