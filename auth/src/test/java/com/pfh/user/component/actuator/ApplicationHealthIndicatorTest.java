package com.pfh.user.component.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pfh.user.actuator.ApplicationHealthIndicator;

class ApplicationHealthIndicatorTest {

    @Test
    @DisplayName("Should return UP when DB connection is valid")
    void shouldReturnUpWhenConnectionIsValid() throws Exception {
        // Mock JDBC components
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        DataSource dataSource = Mockito.mock(DataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);

        // Stub behavior
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.isValid(1)).thenReturn(true);
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/testdb");

        // Create health indicator
        ApplicationHealthIndicator indicator = new ApplicationHealthIndicator(jdbcTemplate, dataSource);

        // Act
        Health health = indicator.health();

        // Assert
        assertEquals("UP", health.getStatus().getCode());
        assertEquals("connected", health.getDetails().get("connectionStatus"));
        assertEquals("localhost", health.getDetails().get("dbHostSource"));
    }

    @Test
    @DisplayName("Should return DOWN when DB connection is invalid")
    void shouldReturnDownWhenConnectionIsInvalid() throws Exception {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        DataSource dataSource = Mockito.mock(DataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.isValid(1)).thenReturn(false);
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/testdb");

        ApplicationHealthIndicator indicator = new ApplicationHealthIndicator(jdbcTemplate, dataSource);

        Health health = indicator.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("disconnected", health.getDetails().get("connectionStatus"));
        assertEquals("localhost", health.getDetails().get("dbHostSource"));
    }

    @Test
    @DisplayName("Should return DOWN when exception occurs during DB check")
    void shouldReturnDownWhenExceptionThrown() throws Exception {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        DataSource dataSource = Mockito.mock(DataSource.class);

        Mockito.when(dataSource.getConnection()).thenThrow(new RuntimeException("DB unreachable"));

        ApplicationHealthIndicator indicator = new ApplicationHealthIndicator(jdbcTemplate, dataSource);

        Health health = indicator.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("disconnected", health.getDetails().get("connectionStatus"));
        assertEquals("unknown-host", health.getDetails().get("dbHostSource"));
        assertTrue(health.getDetails().get("error") != null || health.getDetails().get("exception") != null);
    }
}