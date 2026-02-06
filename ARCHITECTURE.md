# Stran - Event Processing System Architecture

## Overview
Stran is a Spring Boot application that processes events from Kafka, validates subscriptions in a database, and produces notifications to another Kafka topic when active subscriptions exist.

## System Architecture

### High-Level Flow
```
Event Source → Events Topic → Event Consumer → Validation Service → Database
                                      ↓
                           Notification Producer → Notifications Topic → Notification Consumer
```

## Core Components

### 1. **Event Consumer** (`kafka/EventConsumer.java`)
- **Responsibility**: Consumes messages from the events topic
- **Technology**: `@KafkaListener` with Spring Kafka
- **Input**: EventMessage from `events-topic`
- **Configuration**:
  - Consumer Group: `stran-consumer-group`
  - Deserializer: JSON with ErrorHandling
  - Auto-offset: earliest

### 2. **Event Processing Service** (`service/EventProcessingService.java`)
- **Responsibility**: Orchestrates the main business logic
- **Operations**:
  1. Receives event from consumer
  2. Validates subscription existence
  3. Creates notification messages
  4. Triggers notification producer
- **Error Handling**: Logs errors and continues processing

### 3. **Subscription Validation Service** (`service/SubscriptionValidationService.java`)
- **Responsibility**: Validates if events have active subscriptions
- **Operations**:
  - Check subscription existence by event type
  - Retrieve all active subscriptions for an event type
- **Query Criteria**: 
  - Event Type match
  - Active status = true

### 4. **Notification Producer** (`kafka/NotificationProducer.java`)
- **Responsibility**: Publishes notifications to Kafka
- **Technology**: `KafkaTemplate` with Spring Kafka
- **Output**: NotificationMessage to `notifications-topic`
- **Configuration**:
  - Serializer: JSON
  - Acks: all (ensures reliability)
  - Retries: 3

### 5. **Subscription Repository** (`repository/SubscriptionRepository.java`)
- **Responsibility**: Data access layer for subscriptions
- **Technology**: Spring Data JPA
- **Operations**:
  - Find by event type and active status
  - Check existence of active subscriptions

## Data Models

### Subscription Entity
```
subscriptions
├── id (BIGSERIAL, PK)
├── event_type (VARCHAR, UNIQUE, NOT NULL)
├── subscriber_email (VARCHAR, NOT NULL)
├── active (BOOLEAN, DEFAULT true)
├── created_at (TIMESTAMP)
└── updated_at (TIMESTAMP)
```

**Purpose**: Stores subscription configurations for event types

### EventMessage DTO
```json
{
  "eventId": "UUID",
  "eventType": "string",
  "payload": "JSON string",
  "timestamp": "ISO-8601 datetime"
}
```

**Source**: Consumed from `events-topic`

### NotificationMessage DTO
```json
{
  "notificationId": "UUID",
  "eventId": "UUID",
  "eventType": "string",
  "subscriberEmail": "email",
  "message": "string",
  "timestamp": "ISO-8601 datetime"
}
```

**Destination**: Published to `notifications-topic`

## Kafka Topics

### events-topic (Input)
- **Purpose**: Receives events from external systems
- **Message Type**: EventMessage
- **Partitions**: Configurable (suggested: 3-6 based on load)
- **Retention**: Configurable (suggested: 7 days)

### notifications-topic (Output)
- **Purpose**: Publishes notifications for processing
- **Message Type**: NotificationMessage
- **Partitions**: Configurable
- **Retention**: Configurable

## Configuration Layers

### Application Profiles
1. **Default** (`application.properties`): H2 in-memory database for local development
2. **Production** (`application-prod.properties`): PostgreSQL with environment variables

### Key Configuration Parameters
```properties
# Kafka
kafka.bootstrap-servers
kafka.topic.events
kafka.topic.notifications
kafka.consumer.group-id

# Database
spring.datasource.url
spring.datasource.username
spring.datasource.password
spring.jpa.hibernate.ddl-auto
```

## Processing Flow

### Happy Path
1. Event arrives on `events-topic`
2. Consumer deserializes EventMessage
3. Event Processing Service receives event
4. Validation Service queries database for active subscription
5. If subscription exists:
   - Retrieve all matching subscriptions
   - Create NotificationMessage for each subscriber
   - Producer sends notifications to `notifications-topic`
6. If no subscription: Log and skip

### Error Scenarios
- **Deserialization Error**: ErrorHandlingDeserializer logs and skips
- **Database Unavailable**: Exception logged, event reprocessed
- **Kafka Producer Error**: Retries 3 times, then logs failure
- **Invalid Event Data**: Caught and logged, continues processing

## Scalability Considerations

### Horizontal Scaling
- Multiple instances can run with same consumer group
- Kafka partitions distribute load across instances
- Database connection pooling (HikariCP)

### Performance
- Async notification sending (CompletableFuture)
- Batch processing capability (configurable)
- JPA query optimization with indexes

### Recommended Indexes
```sql
CREATE INDEX idx_subscription_event_type_active ON subscriptions(event_type, active);
```

## Technology Stack

- **Framework**: Spring Boot 3.2.2
- **Java Version**: 17
- **Messaging**: Apache Kafka with Spring Kafka
- **Database**: PostgreSQL (prod), H2 (dev)
- **ORM**: Spring Data JPA / Hibernate
- **Build Tool**: Maven
- **Serialization**: Jackson JSON

## Deployment Architecture

### Local Development
```
Stran App (port 8080) → Kafka (localhost:9092) → H2 (in-memory)
```

### Production
```
Stran App Cluster → Kafka Cluster → PostgreSQL
      ↓                   ↓              ↓
Load Balancer    Zookeeper Ensemble   DB Replica
```

## Monitoring & Observability

### Logging
- **Levels**: DEBUG for development, INFO for production
- **Key Events**:
  - Event consumption
  - Subscription validation results
  - Notification production
  - Errors and exceptions

### Metrics to Monitor
- Kafka consumer lag
- Event processing rate
- Database query performance
- Notification production success rate
- Error rates

### Health Checks
- Spring Boot Actuator endpoints
- Kafka consumer health
- Database connectivity
- Topic availability

## Security Considerations

1. **Kafka Security**: Configure SASL/SSL for production
2. **Database**: Connection encryption, credential management
3. **Data Validation**: Input validation for event payloads
4. **Secrets Management**: Use environment variables or secret managers

## Future Enhancements

1. **Dead Letter Queue**: For failed event processing
2. **Idempotency**: Event deduplication using eventId
3. **Rate Limiting**: Per subscription notification limits
4. **Filtering**: Advanced subscription rules (beyond event type)
5. **Metrics Dashboard**: Grafana/Prometheus integration
6. **Audit Trail**: Track all notification sends
7. **Batch Notifications**: Aggregate multiple events

## Development Guidelines

### Adding New Features
1. Create DTOs in `dto/` package
2. Add services in `service/` package
3. Update configuration in `config/` package
4. Add tests in `test/` mirror structure

### Testing Strategy
- Unit tests for services (mocked dependencies)
- Integration tests for Kafka consumers/producers
- Repository tests with H2
- End-to-end tests with Testcontainers
