package com.api.batterymantra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalRedirectFilterTest {

    private CanonicalRedirectFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CanonicalRedirectFilter();
    }

    @Test
    void shouldRedirectWwwToNonWwwHttps() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("www.batterymantra.com");
        request.setRequestURI("/product/exide-mileage");
        request.setQueryString("ref=promo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, response.getStatus());
        assertEquals("https://batterymantra.com/product/exide-mileage?ref=promo", response.getHeader("Location"));
    }

    @Test
    void shouldRedirectBatterymantraInToCanonicalCom() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("batterymantra.in");
        request.setRequestURI("/categories");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, response.getStatus());
        assertEquals("https://batterymantra.com/categories", response.getHeader("Location"));
    }

    @Test
    void shouldRedirectWwwBatterymantraInToCanonicalCom() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("www.batterymantra.in");
        request.setRequestURI("/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, response.getStatus());
        assertEquals("https://batterymantra.com/", response.getHeader("Location"));
    }

    @Test
    void shouldRedirectHttpToHttpsForCanonicalHost() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("batterymantra.com");
        request.setScheme("http");
        request.addHeader("X-Forwarded-Proto", "http");
        request.setRequestURI("/about-us");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, response.getStatus());
        assertEquals("https://batterymantra.com/about-us", response.getHeader("Location"));
    }

    @Test
    void shouldNotRedirectCanonicalHttpsRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("batterymantra.com");
        request.setScheme("https");
        request.addHeader("X-Forwarded-Proto", "https");
        request.setRequestURI("/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertNull(response.getHeader("Location"));
    }

    @Test
    void shouldNotRedirectLocalhostRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setScheme("http");
        request.setRequestURI("/api/categories");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertNull(response.getHeader("Location"));
    }
}

