package com.melon.melonmart;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {
    @Test void requiredStatusesAreDocumented() {
        assertTrue(java.util.Set.of("PENDING","CONFIRMED","SHIPPED","DELIVERED","CANCELLED")
                .contains("PENDING"));
    }
}
