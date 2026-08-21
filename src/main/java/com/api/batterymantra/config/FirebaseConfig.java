package com.api.batterymantra.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                // Try to load from root directory first
                InputStream serviceAccount = null;
                try {
                    serviceAccount = new FileInputStream("serviceAccountKey.json");
                } catch (Exception e) {
                    log.warn("Could not find serviceAccountKey.json in root directory. Trying classpath.");
                    serviceAccount = getClass().getClassLoader().getResourceAsStream("serviceAccountKey.json");
                }

                if (serviceAccount == null) {
                    log.error("Could not find serviceAccountKey.json. Firebase will not be initialized.");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");
            }
        } catch (IOException e) {
            log.error("Error initializing Firebase Admin SDK", e);
        }
    }
}
