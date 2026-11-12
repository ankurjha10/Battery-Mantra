package com.api.batterymantra.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminGateSchemaMigratorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Test
    @DisplayName("Drops NOT NULL constraints on admin_gate_config tier PIN columns for PostgreSQL")
    void testDropsNotNullOnPostgres() throws Exception {
        when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        AdminGateSchemaMigrator migrator = new AdminGateSchemaMigrator(jdbcTemplate);
        migrator.ensureNullableTierPinColumns();

        verify(jdbcTemplate).execute("ALTER TABLE IF EXISTS admin_gate_config ALTER COLUMN admin_pin_hash DROP NOT NULL");
        verify(jdbcTemplate).execute("ALTER TABLE IF EXISTS admin_gate_config ALTER COLUMN sub_admin_pin_hash DROP NOT NULL");
    }

    @Test
    @DisplayName("Skips PostgreSQL DDL statements on non-PostgreSQL databases")
    void testSkipsNonPostgres() throws Exception {
        when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        AdminGateSchemaMigrator migrator = new AdminGateSchemaMigrator(jdbcTemplate);
        migrator.ensureNullableTierPinColumns();

        verify(jdbcTemplate, never()).execute(anyString());
    }
}

