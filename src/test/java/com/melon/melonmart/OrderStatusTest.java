package com.melon.melonmart;

import com.melon.melonmart.service.OrderStatusPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {
    @Test
    void acceptsStatusesSupportedByTheSellerWorkflow() {
        assertTrue(OrderStatusPolicy.isAllowedUpdateStatus("PENDING"));
        assertTrue(OrderStatusPolicy.isAllowedUpdateStatus("CONFIRMED"));
        assertTrue(OrderStatusPolicy.isAllowedUpdateStatus("SHIPPED"));
        assertTrue(OrderStatusPolicy.isAllowedUpdateStatus("DELIVERED"));
    }

    @Test
    void rejectsUnsupportedOrBlankStatuses() {
        assertFalse(OrderStatusPolicy.isAllowedUpdateStatus("CANCELLED"));
        assertFalse(OrderStatusPolicy.isAllowedUpdateStatus("NOT_A_STATUS"));
        assertFalse(OrderStatusPolicy.isAllowedUpdateStatus(" "));
        assertFalse(OrderStatusPolicy.isAllowedUpdateStatus(null));
    }

    @Test
    void normalizesCaseAndWhitespace() {
        assertTrue(OrderStatusPolicy.isAllowedUpdateStatus(" shipped "));
    }
}
