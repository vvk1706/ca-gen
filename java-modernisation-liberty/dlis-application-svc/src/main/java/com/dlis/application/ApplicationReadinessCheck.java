package com.dlis.application;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;

@Readiness
@ApplicationScoped
public class ApplicationReadinessCheck implements HealthCheck {

    @Resource(lookup = "jdbc/dlisDS")
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            return HealthCheckResponse.named("dlis-application-svc-ready")
                    .state(valid)
                    .withData("database", valid ? "UP" : "DOWN")
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.named("dlis-application-svc-ready")
                    .down()
                    .withData("database", "DOWN: " + e.getMessage())
                    .build();
        }
    }
}
