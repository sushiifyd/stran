package com.example.stran.service;

import com.example.stran.dto.inventory.RoomRecommendation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LengthOfStayPatternUtilTest {

    @Nested
    @DisplayName("isAvailableForNights")
    class IsAvailableForNights {

        @Test
        void returnsTrue_whenRoomHasAvailabilityForRequestedNights() {
            RoomRecommendation room = RoomRecommendation.builder()
                    .roomTypeCode("KING")
                    .lengthOfStayPattern(List.of("YYYNNNN"))
                    .build();

            assertThat(LengthOfStayPatternUtil.isAvailableForNights(List.of(room), 2)).isTrue();
        }

        @Test
        void returnsFalse_whenNoRoomHasAvailabilityForRequestedNights() {
            RoomRecommendation room = RoomRecommendation.builder()
                    .roomTypeCode("KING")
                    .lengthOfStayPattern(List.of("YYYNNNN"))
                    .build();

            assertThat(LengthOfStayPatternUtil.isAvailableForNights(List.of(room), 5)).isFalse();
        }

        @Test
        void returnsTrue_whenAnyRoomHasAvailability() {
            RoomRecommendation noAvail = RoomRecommendation.builder()
                    .roomTypeCode("KING")
                    .lengthOfStayPattern(List.of("NNNNNNN"))
                    .build();
            RoomRecommendation hasAvail = RoomRecommendation.builder()
                    .roomTypeCode("QUEEN")
                    .lengthOfStayPattern(List.of("YYYNNNN"))
                    .build();

            assertThat(LengthOfStayPatternUtil.isAvailableForNights(List.of(noAvail, hasAvail), 1)).isTrue();
        }

        @Test
        void returnsFalse_whenRecommendationsIsNull() {
            assertThat(LengthOfStayPatternUtil.isAvailableForNights(null, 1)).isFalse();
        }

        @Test
        void returnsFalse_whenRecommendationsIsEmpty() {
            assertThat(LengthOfStayPatternUtil.isAvailableForNights(Collections.emptyList(), 1)).isFalse();
        }

        @Test
        void returnsFalse_whenNightsIsZero() {
            RoomRecommendation room = RoomRecommendation.builder()
                    .roomTypeCode("KING")
                    .lengthOfStayPattern(List.of("YYYYYYY"))
                    .build();

            assertThat(LengthOfStayPatternUtil.isAvailableForNights(List.of(room), 0)).isFalse();
        }

        @Test
        void returnsFalse_whenNightsExceedsMaxLength() {
            RoomRecommendation room = RoomRecommendation.builder()
                    .roomTypeCode("KING")
                    .lengthOfStayPattern(List.of("YYYYYYY"))
                    .build();

            assertThat(LengthOfStayPatternUtil.isAvailableForNights(List.of(room), 8)).isFalse();
        }

        @Test
        void returnsTrue_forExactly7Nights() {
            RoomRecommendation room = RoomRecommendation.builder()
                    .roomTypeCode("KING")
                    .lengthOfStayPattern(List.of("NNNNNNY"))
                    .build();

            assertThat(LengthOfStayPatternUtil.isAvailableForNights(List.of(room), 7)).isTrue();
        }

        @Test
        void returnsFalse_whenPatternListIsNull() {
            RoomRecommendation room = RoomRecommendation.builder()
                    .roomTypeCode("KING")
                    .lengthOfStayPattern(null)
                    .build();

            assertThat(LengthOfStayPatternUtil.isAvailableForNights(List.of(room), 1)).isFalse();
        }
    }

    @Nested
    @DisplayName("hasAvailability")
    class HasAvailability {

        @Test
        void returnsTrue_whenPatternHasYAtRequestedPosition() {
            assertThat(LengthOfStayPatternUtil.hasAvailability(List.of("YNNNNNN"), 1)).isTrue();
        }

        @Test
        void returnsFalse_whenPatternHasNAtRequestedPosition() {
            assertThat(LengthOfStayPatternUtil.hasAvailability(List.of("NNNNNNN"), 1)).isFalse();
        }

        @Test
        void returnsFalse_whenPatternsIsNull() {
            assertThat(LengthOfStayPatternUtil.hasAvailability(null, 1)).isFalse();
        }

        @Test
        void returnsFalse_whenPatternsIsEmpty() {
            assertThat(LengthOfStayPatternUtil.hasAvailability(Collections.emptyList(), 1)).isFalse();
        }

        @Test
        void returnsTrue_whenAnyPatternHasAvailability() {
            assertThat(LengthOfStayPatternUtil.hasAvailability(List.of("NNNNNNN", "YNNNNNN"), 1)).isTrue();
        }
    }
}
