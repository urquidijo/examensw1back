package com.parcial1.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) return;

        String firebaseJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");

        InputStream serviceAccount;

        if (firebaseJson != null && !firebaseJson.isBlank()) {
            serviceAccount = new ByteArrayInputStream(
                    firebaseJson.getBytes(StandardCharsets.UTF_8)
            );
        } else {
            serviceAccount = getClass()
                    .getClassLoader()
                    .getResourceAsStream("firebase-service-account.json");
        }

        if (serviceAccount == null) {
            throw new IllegalStateException("No se encontró configuración de Firebase");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
    }
}