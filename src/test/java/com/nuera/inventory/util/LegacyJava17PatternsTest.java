package com.nuera.inventory.util;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class LegacyJava17PatternsTest {

    private final LegacyJava17Patterns helper = new LegacyJava17Patterns();

    @Test
    void testCreateLocale() {
        Locale loc = helper.createLocale("en", "US");
        assertNotNull(loc);
        assertEquals("en", loc.getLanguage());
        assertEquals("US", loc.getCountry());
    }

    @Test
    void testCreateHttpUrl() throws MalformedURLException {
        URL url = helper.createHttpUrl("https://localhost:8080/api/products");
        assertNotNull(url);
        assertEquals("https", url.getProtocol());
        assertEquals("localhost", url.getHost());
    }

    @Test
    void testGetFirstAndLastElement() {
        List<String> items = List.of("alpha", "beta", "gamma");
        assertEquals("alpha -> gamma", helper.getFirstAndLastElement(items));
        assertEquals("EMPTY", helper.getFirstAndLastElement(List.of()));
    }

    @Test
    void testClassifyCategory() {
        assertEquals("TECH", helper.classifyCategory("ELECTRONICS"));
        assertEquals("HOME", helper.classifyCategory("FURNITURE"));
        assertEquals("GROCERY", helper.classifyCategory("FOOD"));
        assertEquals("GENERAL", helper.classifyCategory("OTHER"));
    }
}
