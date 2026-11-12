package com.api.batterymantra.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Locale;

/**
 * Ensures PostgreSQL drops stale NOT NULL constraints on admin_gate_config PIN columns
 * when updating an existing schema created before those columns became nullable.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdminGateSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureNullableTierPinColumns() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            if (productName == null || !productName.toLowerCase(Locale.ROOT).contains("postgresql")) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE IF EXISTS admin_gate_config ALTER COLUMN admin_pin_hash DROP NOT NULL");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS admin_gate_config ALTER COLUMN sub_admin_pin_hash DROP NOT NULL");
        } catch (Exception ex) {
            log.warn("Could not verify nullable constraints on admin_gate_config: {}", ex.getMessage());
        }
    }
}

