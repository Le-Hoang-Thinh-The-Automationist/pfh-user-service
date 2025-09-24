package com.pfh.user.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;

@Component("db")
public class ApplicationHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final String dbHost;
    private DataSource dataSource;

    public ApplicationHealthIndicator(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.dbHost = extractHostFromJdbcUrl(dataSource);

    }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1)) {
                return Health.up()
                        .withDetail("connectionStatus", "connected")
                        .withDetail("dbHostSource", dbHost)
                        .build();
            } else {
                return Health.down()
                        .withDetail("connectionStatus", "disconnected")
                        .withDetail("dbHostSource", dbHost)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("connectionStatus", "disconnected")
                    .withDetail("dbHostSource", dbHost)
                    .withException(e)
                    .build();
        }
    }

    private String extractHostFromJdbcUrl(DataSource dataSource) {
        try {
            String url = dataSource.getConnection().getMetaData().getURL();
            URI uri = new URI(url.replace("jdbc:", ""));
            return uri.getHost() != null ? uri.getHost() : "unknown-host";
        } catch (Exception e) {
            return "unknown-host";
        }
    }
}
