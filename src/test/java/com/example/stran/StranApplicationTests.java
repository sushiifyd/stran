package com.example.stran;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"test-rate-recomm", "test-stran-notifications"})
@TestPropertySource(locations = "classpath:application-test.properties")
class StranApplicationTests {

    @Test
    void contextLoads() {
    }
}
