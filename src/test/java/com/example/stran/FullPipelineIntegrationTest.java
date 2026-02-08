package com.example.stran;

import com.example.stran.dto.inventory.*;
import com.example.stran.dto.notification.NotificationMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for the full event processing pipeline:
 * Kafka consumer → matching engine → notification dispatch → Kafka producer.
 *
 * <p>Uses embedded Kafka + H2 to verify the entire flow without external dependencies.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"test-rate-recomm", "test-stran-notifications"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
)
@DirtiesContext
@org.springframework.context.annotation.Import(TestKafkaProducerConfig.class)
class FullPipelineIntegrationTest {

    @Autowired
    private KafkaTemplate<String, InventoryEvent> inventoryKafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedDatabase() {
        // Create the hmstst schema and seed test data (auto-commits so Kafka listener threads can see it)
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS hmstst");

        // Insert test property
        jdbcTemplate.execute(
                "MERGE INTO hmstst.property (property_id, title, ctyhocn, latitude, longitude, brand, prop_code, created_date, updated_date, persistence_version) " +
                "VALUES (1, 'Test Hotel', 'FNLCOHF', 40.0, -74.0, 'HF', 'FNLCO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)");

        // Insert test subscription — 3 nights starting 2026-03-09
        jdbcTemplate.execute(
                "MERGE INTO stay_subscriptions (id, guest_id, search_type, property_id, check_in_date, check_out_date, " +
                "max_price_per_night, currency_code, num_adults, num_rooms, status, created_at, updated_at, check_count) " +
                "VALUES (100, 'guest-123', 'PROPERTY', 1, '2026-03-09', '2026-03-12', 200.00, 'USD', 2, 1, 'ACTIVE', " +
                "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)");
    }

    @Test
    @DisplayName("Full pipeline: inventory event → matching → notification published to Kafka")
    void fullPipeline_publishesNotificationForMatchingSubscription() throws Exception {
        // Arrange: build an inventory event with LOS pattern supporting 3 nights
        RoomRecommendation room = RoomRecommendation.builder()
                .roomTypeCode("KING")
                .lengthOfStayPattern(List.of("YYYNNNN"))
                .build();

        InventoryEventBody body = InventoryEventBody.builder()
                .recommendationId(1L)
                .propCode("FNLCO")
                .startDate("2026-03-09")
                .endDate("2026-03-09")
                .ratePlanCode("NG7BCD")
                .roomRecommendations(List.of(room))
                .build();

        InventoryEventValue value = InventoryEventValue.builder()
                .context(EventContext.builder()
                        .timestamp("2026-03-09T10:00:00Z")
                        .publisher("test")
                        .messageId("msg-001")
                        .build())
                .body(body)
                .build();

        InventoryEvent event = InventoryEvent.builder()
                .key("FNLCO::NG7BCD")
                .value(value)
                .build();

        // Set up a consumer to read from the notifications topic
        Consumer<String, NotificationMessage> consumer = createNotificationConsumer();

        // Act: send the inventory event to the input topic
        inventoryKafkaTemplate.send("test-rate-recomm", "FNLCO::NG7BCD", event).get();

        // Assert: read from the notifications topic
        ConsumerRecords<String, NotificationMessage> records =
                KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));

        assertThat(records.count()).isGreaterThanOrEqualTo(1);

        NotificationMessage notification = records.iterator().next().value();
        assertThat(notification.getSubscriptionId()).isEqualTo(100L);
        assertThat(notification.getGuestId()).isEqualTo("guest-123");
        assertThat(notification.getPropCode()).isEqualTo("FNLCO");
        assertThat(notification.getCheckInDate()).isEqualTo("2026-03-09");
        assertThat(notification.getNights()).isEqualTo(3);
        assertThat(notification.getNotificationId()).isNotBlank();
        assertThat(notification.getMessage()).contains("FNLCO", "3 night(s)");

        consumer.close();
    }

    @Test
    @DisplayName("No notification published when LOS pattern does not match subscription nights")
    void fullPipeline_noNotificationWhenLOSDoesNotMatch() throws Exception {
        // Seed a separate property + subscription to isolate from other tests
        jdbcTemplate.execute(
                "MERGE INTO hmstst.property (property_id, title, ctyhocn, latitude, longitude, brand, prop_code, created_date, updated_date, persistence_version) " +
                "VALUES (2, 'Isolated Hotel', 'ISOLHF', 41.0, -75.0, 'HF', 'ISOL1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)");
        jdbcTemplate.execute(
                "MERGE INTO stay_subscriptions (id, guest_id, search_type, property_id, check_in_date, check_out_date, " +
                "max_price_per_night, currency_code, num_adults, num_rooms, status, created_at, updated_at, check_count) " +
                "VALUES (200, 'guest-456', 'PROPERTY', 2, '2026-03-09', '2026-03-12', 200.00, 'USD', 2, 1, 'ACTIVE', " +
                "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)");

        // LOS pattern only supports 1 night, but subscription wants 3
        RoomRecommendation room = RoomRecommendation.builder()
                .roomTypeCode("KING")
                .lengthOfStayPattern(List.of("YNNNNNN"))
                .build();

        InventoryEventBody body = InventoryEventBody.builder()
                .recommendationId(2L)
                .propCode("ISOL1")
                .startDate("2026-03-09")
                .endDate("2026-03-09")
                .ratePlanCode("NG7BCD")
                .roomRecommendations(List.of(room))
                .build();

        InventoryEvent event = InventoryEvent.builder()
                .key("ISOL1::NG7BCD")
                .value(InventoryEventValue.builder()
                        .context(EventContext.builder()
                                .timestamp("2026-03-09T10:00:00Z")
                                .publisher("test")
                                .messageId("msg-002")
                                .build())
                        .body(body)
                        .build())
                .build();

        Consumer<String, NotificationMessage> consumer = createNotificationConsumer();

        inventoryKafkaTemplate.send("test-rate-recomm", "ISOL1::NG7BCD", event).get();

        // Wait and verify no notification was published for this isolated subscription
        ConsumerRecords<String, NotificationMessage> records =
                KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

        // Only check for notifications matching the isolated subscription
        long matchingRecords = 0;
        var it = records.iterator();
        while (it.hasNext()) {
            var rec = it.next();
            if (rec.value() != null && rec.value().getSubscriptionId() != null
                    && rec.value().getSubscriptionId() == 200L) {
                matchingRecords++;
            }
        }
        assertThat(matchingRecords).isZero();

        consumer.close();
    }

    private Consumer<String, NotificationMessage> createNotificationConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-notification-consumer", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.stran.dto.notification");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, NotificationMessage.class.getName());
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        ConsumerFactory<String, NotificationMessage> cf = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(),
                new JsonDeserializer<>(NotificationMessage.class, false));

        Consumer<String, NotificationMessage> consumer = cf.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "test-stran-notifications");
        return consumer;
    }
}
