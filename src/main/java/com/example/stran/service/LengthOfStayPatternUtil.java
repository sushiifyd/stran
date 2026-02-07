package com.example.stran.service;

import com.example.stran.dto.inventory.RoomRecommendation;

import java.util.List;

/**
 * Utility for evaluating length-of-stay (LOS) patterns from inventory events.
 *
 * <p>Each LOS pattern is a 7-character string where position N (1-indexed)
 * indicates whether a stay of N nights is available:
 * <ul>
 *   <li>{@code 'Y'} = available</li>
 *   <li>{@code 'N'} = not available</li>
 * </ul>
 *
 * <p>Example: {@code "YYYNNNN"} → 1, 2, or 3 nights available.
 */
public final class LengthOfStayPatternUtil {

    private static final int MAX_LOS_LENGTH = 7;

    private LengthOfStayPatternUtil() {
        // utility class
    }

    /**
     * Check if any room type in the recommendations supports the requested number of nights.
     *
     * @param recommendations list of room recommendations from the inventory event
     * @param nights the number of nights the guest wants (1-7)
     * @return true if at least one room type has availability for the requested nights
     */
    public static boolean isAvailableForNights(List<RoomRecommendation> recommendations, int nights) {
        if (recommendations == null || recommendations.isEmpty()) {
            return false;
        }
        if (nights < 1 || nights > MAX_LOS_LENGTH) {
            return false;
        }

        return recommendations.stream()
                .anyMatch(rec -> hasAvailability(rec.getLengthOfStayPattern(), nights));
    }

    /**
     * Check if a single LOS pattern list supports the requested number of nights.
     *
     * @param patterns the LOS pattern strings (typically a single-element list)
     * @param nights the number of nights (1-7)
     * @return true if any pattern indicates availability for the requested nights
     */
    static boolean hasAvailability(List<String> patterns, int nights) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }

        int index = nights - 1; // 0-indexed position

        return patterns.stream()
                .anyMatch(pattern -> isPositionAvailable(pattern, index));
    }

    /**
     * Check if a specific position in a LOS pattern string is 'Y'.
     *
     * @param pattern the LOS pattern string (e.g., "YYYNNNN")
     * @param index the 0-based index to check
     * @return true if the position is 'Y'
     */
    private static boolean isPositionAvailable(String pattern, int index) {
        if (pattern == null || index < 0 || index >= pattern.length()) {
            return false;
        }
        return pattern.charAt(index) == 'Y';
    }
}
