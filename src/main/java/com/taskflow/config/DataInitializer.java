package com.taskflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initialiseur de données — production.
 * Aucune donnée de démonstration n'est injectée.
 * Les comptes sont créés via /register.
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("✅ TaskFlow démarré — mode production, aucune donnée de test injectée.");
    }
}
