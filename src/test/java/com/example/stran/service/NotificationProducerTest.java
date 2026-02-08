package com.example.stran.service;

import com.example.stran.dto.notification.NotificationMessage;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    @Mock
    private KafkaTemplate<String, NotificationMessage> kafkaTemplate;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<NotificationMessage> messageCaptor;

    private NotificationProducer producer;

    private NotificationMessage testMessage;

    @BeforeEach
    void setUp() {
        producer = new NotificationProducer(kafkaTemplate, "stran-notifications");

        testMessage = NotificationMessage.builder()
                .notificationId("notif-001")
                .subscriptionId(100L)
                .guestId("guest-123")
                .propCode("FNLCO")
                .checkInDate("2026-03-09")
                .nights(3)
                .message("Availability found at property FNLCO for 3 night(s) starting 2026-03-09")
                .timestamp(Instant.now())
                .build();
    }

    @Test
    @DisplayName("sends message to the correct topic with subscription ID as key")
    void sendSingle_sendsToCorrectTopicWithKey() {
        CompletableFuture<SendResult<String, NotificationMessage>> future = new CompletableFuture<>();
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("stran-notifications", 0), 0, 0, 0, 0, 0);
        ProducerRecord<String, NotificationMessage> record = new ProducerRecord<>("stran-notifications", "100", testMessage);
        future.complete(new SendResult<>(record, metadata));

        when(kafkaTemplate.send(any(String.class), any(String.class), any(NotificationMessage.class)))
                .thenReturn(future);

        producer.sendSingle(testMessage);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), messageCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo("stran-notifications");
        assertThat(keyCaptor.getValue()).isEqualTo("100");
        assertThat(messageCaptor.getValue()).isEqualTo(testMessage);
    }

    @Test
    @DisplayName("send publishes all messages in the list")
    void send_publishesAllMessages() {
        NotificationMessage msg2 = NotificationMessage.builder()
                .notificationId("notif-002")
                .subscriptionId(101L)
                .guestId("guest-456")
                .propCode("FNLCO")
                .checkInDate("2026-03-09")
                .nights(2)
                .message("Availability found")
                .timestamp(Instant.now())
                .build();

        CompletableFuture<SendResult<String, NotificationMessage>> future = new CompletableFuture<>();
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("stran-notifications", 0), 0, 0, 0, 0, 0);
        ProducerRecord<String, NotificationMessage> record = new ProducerRecord<>("stran-notifications", "100", testMessage);
        future.complete(new SendResult<>(record, metadata));

        when(kafkaTemplate.send(any(String.class), any(String.class), any(NotificationMessage.class)))
                .thenReturn(future);

        producer.send(List.of(testMessage, msg2));

        verify(kafkaTemplate, times(2)).send(eq("stran-notifications"), any(String.class), any(NotificationMessage.class));
    }

    @Test
    @DisplayName("handles send failure gracefully via callback logging")
    void sendSingle_handlesFailureGracefully() {
        CompletableFuture<SendResult<String, NotificationMessage>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Broker unreachable"));

        when(kafkaTemplate.send(any(String.class), any(String.class), any(NotificationMessage.class)))
                .thenReturn(future);

        // Should not throw — failure is handled in whenComplete callback
        producer.sendSingle(testMessage);

        verify(kafkaTemplate).send("stran-notifications", "100", testMessage);
    }
}
