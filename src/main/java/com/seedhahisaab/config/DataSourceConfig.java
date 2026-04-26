package com.seedhahisaab.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(Environment environment) {
        // Try to get DATABASE_URL from environment (Docker)
        String dbUrl = System.getenv("DATABASE_URL");
        
        // Fallback to application properties if DATABASE_URL not set (local development)
        if (dbUrl == null || dbUrl.isBlank()) {
            String host = environment.getProperty("spring.datasource.host", "localhost");
            String port = environment.getProperty("spring.datasource.port", "5432");
            String database = environment.getProperty("spring.datasource.database", "seedhahisaab");
            String username = environment.getProperty("spring.datasource.username", "seedhahisaab");
            String password = environment.getProperty("spring.datasource.password", "seedhahisaab");
            
            dbUrl = String.format("postgresql://%s:%s@%s:%s/%s", username, password, host, port, database);
        }
        
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalStateException(
                    "DATABASE_URL environment variable or spring.datasource properties not configured. " +
                    "For Docker: Set DATABASE_URL environment variable. " +
                    "For local: Set spring.datasource.* properties in application.properties or environment variables.");
        }

        try {
            return createDataSourceFromUrl(dbUrl);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to configure database. Check DATABASE_URL format. " +
                    "Expected format: postgresql://user:password@host:port/database. " +
                    "Error: " + e.getMessage(), e);
        }
    }

    private DataSource createDataSourceFromUrl(String dbUrl) {
        URI uri = URI.create(dbUrl);
        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String path = uri.getPath();
        String query = uri.getRawQuery();
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
        
        if (query != null && !query.isBlank()) {
            jdbcUrl += "?" + query;
        }

        String username = "";
        String password = "";
        String userInfo = uri.getUserInfo();
        
        if (userInfo != null && !userInfo.isBlank()) {
            String[] parts = userInfo.split(":", 2);
            username = parts[0];
            password = parts.length > 1 ? parts[1] : "";
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(20000);
        ds.setIdleTimeout(300000);
        ds.setMaxLifetime(1200000);
        
        System.out.println("✅ Database configured: " + jdbcUrl.replaceAll("password=[^&;]*", "password=****"));
        
        return ds;
    }
}

