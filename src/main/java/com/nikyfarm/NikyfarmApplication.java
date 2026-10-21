package com.nikyfarm;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NikyfarmApplication {

    public static void main(String[] args) {
        loadDotEnvIntoSystemProperties();
        SpringApplication.run(NikyfarmApplication.class, args);
    }

    /** Loads local .env values before Spring Boot initializes its environment. */
    private static void loadDotEnvIntoSystemProperties() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        for (DotenvEntry entry : dotenv.entries()) {
            String envKey = entry.getKey();
            String propertyKey = "SPRING_PROFILES_ACTIVE".equals(envKey)
                    ? "spring.profiles.active"
                    : envKey;

            // Preserve values explicitly supplied by the OS or JVM.
            if (System.getenv(envKey) == null
                    && System.getProperty(propertyKey) == null) {
                System.setProperty(propertyKey, entry.getValue());
            }
        }
    }
}

