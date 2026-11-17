package com.nuera.inventory.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Demonstrates pre-Java 21 idioms for OpenRewrite modernization validation:
 * - Deprecated URL constructors (deprecated in Java 20)
 * - Deprecated Locale constructors (deprecated in Java 19)
 * - SequencedCollection list element access (introduced in Java 21)
 * - Redundant semicolons
 */
public class LegacyJava17Patterns {

    public Locale createLocale(String language, String country) {
        return new Locale(language, country);
    };

    public URL createHttpUrl(String spec) throws MalformedURLException {
        return new URL(spec);
    }

    public String getFirstAndLastElement(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "EMPTY";
        }
        String first = items.get(0);
        String last = items.get(items.size() - 1);
        return first + " -> " + last;
    }

    public String classifyCategory(String category) {
        String result;
        if ("ELECTRONICS".equalsIgnoreCase(category)) {
            result = "TECH";
        } else if ("FURNITURE".equalsIgnoreCase(category)) {
            result = "HOME";
        } else if ("FOOD".equalsIgnoreCase(category)) {
            result = "GROCERY";
        } else {
            result = "GENERAL";
        }
        return result;
    }
}
