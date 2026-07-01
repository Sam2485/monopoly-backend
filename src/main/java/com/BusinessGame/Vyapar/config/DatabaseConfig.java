package com.BusinessGame.Vyapar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        String url = getEnv("DATABASE_URL", "jdbc:postgresql://localhost:5432/monopoly");
        String username = getEnv("DB_USERNAME", "rajshaikh");
        String password = getEnv("DB_PASSWORD", "Sam@2445");

        System.out.println("[Vyapar DB Config] Initializing datasource URL: " + url);
        System.out.println("[Vyapar DB Config] Initializing datasource Username: " + username);

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    private String getEnv(String name, String defaultValue) {
        // 1. Try exact name
        String value = System.getenv(name);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        
        // 2. Try name with a trailing space
        value = System.getenv(name + " ");
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        // 3. Try standard Spring datasource environment variables
        if ("DATABASE_URL".equals(name)) {
            value = System.getenv("SPRING_DATASOURCE_URL");
        } else if ("DB_USERNAME".equals(name)) {
            value = System.getenv("SPRING_DATASOURCE_USERNAME");
        } else if ("DB_PASSWORD".equals(name)) {
            value = System.getenv("SPRING_DATASOURCE_PASSWORD");
        }
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        return defaultValue;
    }
}
