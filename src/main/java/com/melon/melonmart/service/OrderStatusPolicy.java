package com.melon.melonmart.service;

import java.util.Locale;
import java.util.Set;

/**
 * Centralizes the order statuses that may be selected by the current workflow.
 */
public final class OrderStatusPolicy {
    private static final Set<String> UPDATE_STATUSES =
            Set.of("PENDING", "CONFIRMED", "SHIPPED", "DELIVERED");

    private OrderStatusPolicy() {
    }

    public static boolean isAllowedUpdateStatus(String status) {
        return status != null
                && UPDATE_STATUSES.contains(status.trim().toUpperCase(Locale.ROOT));
    }
}
