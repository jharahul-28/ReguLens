package com.rj.ReguLens.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class FlywayConfig {

    @Bean
    public CommandLineRunner flywayMigrationRunner(DataSource dataSource) {
        return args -> {
            log.info("Executing Flyway schema migration on connected datasource...");
            try {
                Flyway flyway = Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .baselineOnMigrate(true)
                        .outOfOrder(true)
                        .load();
                flyway.repair();
                var result = flyway.migrate();
                log.info("Flyway migration finished successfully: {} migrations executed (target version: {})",
                        result.migrationsExecuted, result.targetSchemaVersion);
            } catch (Exception e) {
                log.error("Flyway migration encountered an issue: {}", e.getMessage(), e);
            }
        };
    }
}
