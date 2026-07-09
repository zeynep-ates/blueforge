package com.blueforge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Booting the full context runs Flyway against a real Postgres and lets
// ddl-auto=validate confirm the resulting schema matches every entity mapping.
// Named *IT so Failsafe (not Surefire) picks it up: mvn test stays DB-free,
// mvn verify runs it against the Postgres CI/docker-compose already provision.
@SpringBootTest
class FlywayMigrationIT {

    @Test
    void applicationContextLoadsAgainstMigratedSchema() {}
}
