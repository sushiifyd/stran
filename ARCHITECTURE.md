# Stream Architecture

## Overview
Spring Boot application that listens to Kafka events, validates subscriptions in database, and sends notifications to Kafka when subscriptions exist.

## Flow
```
Kafka Events Topic → Consumer → Validate Subscription (DB) → Producer → Kafka Notifications Topic
```

## Components

### 1. Event Consumer
- Listens to `events-topic` using `@KafkaListener`
- Deserializes JSON to `EventMessage`

### 2. Event Processing Service  
- Checks if subscription exists for event type
- Creates notification for each active subscription
- Sends to Notification Producer

### 3. Subscription Validation Service
- Queries database for active subscriptions by event type
- Returns matching subscriptions

### 4. Notification Producer
- Publishes `NotificationMessage` to `notifications-topic`
- Uses `KafkaTemplate`

### 5. Subscription Repository
- JPA repository for subscription data access

## Data Models

**Subscription Table:**
- `id`, `event_type` (unique), `subscriber_email`, `active`, `created_at`, `updated_at`

**EventMessage (Input):**
- `eventId`, `eventType`, `payload`, `timestamp`

**NotificationMessage (Output):**  
- `notificationId`, `eventId`, `eventType`, `subscriberEmail`, `message`, `timestamp`

## Configuration

**Kafka:**
- `kafka.bootstrap-servers` 
- `kafka.topic.events`
- `kafka.topic.notifications`
- `kafka.consumer.group-id`

**Database:**
- Development: H2 in-memory
- Production: PostgreSQL

## Tech Stack
- Spring Boot 3.2.2
- Java 17
- Spring Kafka
- Spring Data JPA
- Maven
