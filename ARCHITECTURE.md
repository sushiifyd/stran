# Stran — Inventory Matching Engine Architecture

## Overview

Stran is the core matching engine in the STRAN ecosystem. It consumes hotel room/stay inventory events from an **MSK (Amazon Managed Streaming for Apache Kafka)** topic, matches incoming inventory against active guest subscriptions stored in the subscription database, and dispatches notification requests to the **stran-notification-service** when a match is found.

## System Context

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           STRAN Ecosystem                                   │
│                                                                             │
│  ┌──────────────────────┐                                                   │
│  │  stran-subscription  │  Guest creates/manages                            │
│  │      -service        │  stay subscriptions                               │
│  │     (REST API)       │──────────────┐                                    │
│  └──────────────────────┘              │                                    │
│                                        ▼                                    │
│                              ┌──────────────────┐                           │
│                              │   Subscription    │                           │
│  Hotel Inventory             │    Database       │                           │
│  Systems                     │  (PostgreSQL)     │                           │
│       │                      └──────────────────┘                           │
│       │                                ▲                                    │
│       ▼                                │ query                              │
│  ┌──────────┐    consume    ┌──────────┴─────────┐   notify   ┌──────────┐ │
│  │   MSK    │──────────────▶│       stran         │──────────▶│  stran-  │ │
│  │  Topic   │  (inventory   │  (matching engine)  │           │notifica- │ │
│  │          │   events)     │   ◀── this project  │           │tion svc  │ │
│  └──────────┘               └────────────────────┘           └──────────┘ │
│                                                                     │      │
│                                                                     ▼      │
│                                                                  Guest     │
│                                                                notified    │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Internal Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        stran (matching engine)                       │
│                                                                      │
│  ┌───────────────────┐                                               │
│  │  Kafka Consumer   │  Listens to MSK inventory topic               │
│  │  (InventoryEvent  │  Deserializes InventoryEvent messages         │
│  │   Listener)       │                                               │
│  └────────┬──────────┘                                               │
│           │                                                          │
│           ▼                                                          │
│  ┌───────────────────┐     ┌──────────────────────┐                  │
│  │  Inventory Event  │     │   Subscription       │                  │
│  │  Processing       │────▶│   Matching Service   │                  │
│  │  Service          │     │                      │                  │
│  └───────────────────┘     └──────────┬───────────┘                  │
│                                       │                              │
│                            ┌──────────▼───────────┐                  │
│                            │  Subscription        │                  │
│                            │  Repository          │                  │
│                            │  (JPA / DB access)   │                  │
│                            └──────────┬───────────┘                  │
│                                       │                              │
│                            ┌──────────▼───────────┐                  │
│                            │  Subscription DB     │                  │
│                            │  (PostgreSQL / H2)   │                  │
│                            └──────────────────────┘                  │
│                                       │                              │
│                             matching subscriptions                   │
│                                       │                              │
│                            ┌──────────▼───────────┐                  │
│  ┌───────────────────┐     │  Notification        │                  │
│  │  Kafka Producer   │◀────│  Dispatch Service    │                  │
│  │  (Notification    │     │                      │                  │
│  │   Publisher)      │     └──────────────────────┘                  │
│  └────────┬──────────┘                                               │
│           │                                                          │
│           ▼                                                          │
│  Publishes to notifications topic ──▶ stran-notification-service     │
└──────────────────────────────────────────────────────────────────────┘
```

## Components

### 1. Inventory Event Consumer
- Listens to the MSK inventory topic using `@KafkaListener`
- Deserializes JSON messages into `InventoryEvent` DTOs
- Delegates to the processing service

### 2. Inventory Event Processing Service
- Orchestrates the matching workflow
- Extracts matching criteria from the inventory event (property/location, dates, room type)
- Invokes the subscription matching service
- For each match, triggers the notification dispatch

### 3. Subscription Matching Service
- Queries the subscription database for **active** subscriptions that match the incoming inventory
- Matching criteria: location, check-in date, duration/nights, room type
- Returns a list of matching subscriptions

### 4. Subscription Repository
- JPA repository for querying guest subscriptions
- Shared database with stran-subscription-service (read-only access from stran)

### 5. Notification Dispatch Service
- Builds notification messages for each matched subscription
- Delegates publishing to the Kafka producer

### 6. Notification Producer
- Publishes `NotificationMessage` to the notifications MSK topic
- Consumed downstream by stran-notification-service

## Data Flow

```
1. Hotel inventory system publishes a new inventory event to MSK topic
       │
       ▼
2. InventoryEventListener consumes the event
       │
       ▼
3. InventoryEventProcessingService extracts matching criteria:
   - property / location (e.g., "Las Vegas")
   - available dates (e.g., Nov 23)
   - number of nights (e.g., 2)
   - room type
       │
       ▼
4. SubscriptionMatchingService queries DB:
   "Find all active subscriptions where location, dates, and nights match"
       │
       ├── No matches → discard, done
       │
       └── Matches found
               │
               ▼
5. NotificationDispatchService creates NotificationMessage per match
       │
       ▼
6. NotificationProducer publishes to notifications MSK topic
       │
       ▼
7. stran-notification-service delivers notification to guest
```

## Data Models

### InventoryEvent (Input — from MSK topic)
| Field          | Type     | Description                               |
|----------------|----------|-------------------------------------------|
| eventId        | String   | Unique event identifier                   |
| propertyCode   | String   | Hotel property code                       |
| location       | String   | Hotel location / city                     |
| roomType       | String   | Room type (e.g., King, Double Queen)      |
| availableDate  | LocalDate| Date the room is available                |
| nights         | Integer  | Number of consecutive nights available    |
| payload        | Object   | Additional inventory details              |
| timestamp      | Instant  | Event timestamp                           |

### Subscription (Database — shared with stran-subscription-service)
| Field            | Type      | Description                             |
|------------------|-----------|-----------------------------------------|
| id               | Long      | Primary key                             |
| guestId          | String    | Guest identifier                        |
| location         | String    | Desired location                        |
| checkInDate      | LocalDate | Desired check-in date                   |
| nights           | Integer   | Number of nights                        |
| roomType         | String    | Preferred room type (nullable)          |
| active           | Boolean   | Whether subscription is active          |
| subscriberEmail  | String    | Guest email for notification            |
| createdAt        | Instant   | Subscription creation timestamp         |
| updatedAt        | Instant   | Last update timestamp                   |

### NotificationMessage (Output — to MSK topic)
| Field           | Type    | Description                              |
|-----------------|---------|------------------------------------------|
| notificationId  | String  | Unique notification identifier           |
| eventId         | String  | Source inventory event ID                |
| subscriptionId  | Long    | Matched subscription ID                  |
| guestId         | String  | Guest to notify                          |
| subscriberEmail | String  | Guest email                              |
| location        | String  | Matched location                         |
| checkInDate     | String  | Available date                           |
| nights          | Integer | Number of nights                         |
| message         | String  | Human-readable notification message      |
| timestamp       | Instant | Notification creation timestamp          |

## Configuration

### MSK / Kafka
| Property                      | Description                              |
|-------------------------------|------------------------------------------|
| `spring.kafka.bootstrap-servers` | MSK broker endpoints                  |
| `kafka.topic.inventory`       | Inventory events topic (input)           |
| `kafka.topic.notifications`   | Notification messages topic (output)     |
| `kafka.consumer.group-id`     | Consumer group for this engine           |

### Database
| Environment | Engine     | Notes                                     |
|-------------|------------|-------------------------------------------|
| Development | H2         | In-memory, schema auto-created            |
| Production  | PostgreSQL | Shared with stran-subscription-service    |

## Tech Stack

| Technology         | Version | Purpose                          |
|--------------------|---------|----------------------------------|
| Java               | 17      | Language                         |
| Spring Boot        | 3.2.2   | Application framework            |
| Spring Kafka       | —       | MSK/Kafka consumer & producer    |
| Spring Data JPA    | —       | Database access                  |
| PostgreSQL         | —       | Production database              |
| H2                 | —       | Development/test database        |
| Lombok             | —       | Boilerplate reduction            |
| Jackson            | —       | JSON serialization               |
| Maven              | —       | Build tool                       |
