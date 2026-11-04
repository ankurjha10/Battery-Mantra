package com.api.batterymantra.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SeoUtilTest {

    @Test
    void shouldSanitizeWwwCanonicalUrl() {
        assertEquals("https://batterymantra.com/product/amarok",
                SeoUtil.sanitizeCanonicalUrl("https://www.batterymantra.com/product/amarok"));
        assertEquals("https://batterymantra.com/product/amarok",
                SeoUtil.sanitizeCanonicalUrl("http://www.batterymantra.com/product/amarok"));
    }

    @Test
    void shouldSanitizeBatterymantraInCanonicalUrl() {
        assertEquals("https://batterymantra.com/blog/upgrade-battery",
                SeoUtil.sanitizeCanonicalUrl("https://batterymantra.in/blog/upgrade-battery"));
        assertEquals("https://batterymantra.com/blog/upgrade-battery",
                SeoUtil.sanitizeCanonicalUrl("http://batterymantra.in/blog/upgrade-battery"));
        assertEquals("https://batterymantra.com/blog/upgrade-battery",
                SeoUtil.sanitizeCanonicalUrl("https://www.batterymantra.in/blog/upgrade-battery"));
    }

    @Test
    void shouldSanitizeInternalBlogLinksInContent() {
        String html = "<p>Check our <a href=\"https://www.batterymantra.com/inverters\">Inverter Deals</a> and " +
                "<a href=\"https://batterymantra.in/blog/post-1\">blog</a></p>";
        String expected = "<p>Check our <a href=\"https://batterymantra.com/inverters\">Inverter Deals</a> and " +
                "<a href=\"https://batterymantra.com/blog/post-1\">blog</a></p>";

        assertEquals(expected, SeoUtil.sanitizeInternalLinks(html));
    }

    @Test
    void shouldHandleNullAndBlank() {
        assertNull(SeoUtil.sanitizeCanonicalUrl(null));
        assertEquals("", SeoUtil.sanitizeCanonicalUrl(""));
        assertNull(SeoUtil.sanitizeInternalLinks(null));
    }
}

