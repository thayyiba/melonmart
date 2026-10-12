package com.melon.melonmart;

import com.melon.melonmart.ai.WelpServlet;
import com.melon.melonmart.controller.WishlistServlet;
import com.melon.melonmart.controller.DeliveryAddressServlet;
import org.junit.jupiter.api.Test;

import javax.servlet.annotation.WebServlet;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiVersionMappingTest {
    @Test
    void deliveryAddressesExposeLegacyAndVersionedApiRoutes() {
        assertRoutes(DeliveryAddressServlet.class, "/api/addresses", "/api/v1/addresses");
    }

    @Test
    void wishlistExposesLegacyAndVersionedApiRoutes() {
        assertRoutes(WishlistServlet.class, "/api/wishlist", "/api/v1/wishlist");
    }

    @Test
    void welpExposesLegacyAndVersionedApiRoutes() {
        assertRoutes(WelpServlet.class, "/api/welp", "/api/v1/welp");
    }

    private void assertRoutes(Class<?> servletClass, String legacy, String versioned) {
        WebServlet mapping = servletClass.getAnnotation(WebServlet.class);
        assertTrue(mapping != null, servletClass.getSimpleName() + " must declare a servlet mapping");
        List<String> routes = Arrays.asList(mapping.value());
        assertTrue(routes.contains(legacy), "Missing legacy route " + legacy);
        assertTrue(routes.contains(versioned), "Missing versioned route " + versioned);
    }
}
