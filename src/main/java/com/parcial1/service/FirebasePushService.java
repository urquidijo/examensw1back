package com.parcial1.service;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FirebasePushService {

  public void sendPush(String token, String title, String body, Map<String, String> data) {
    if (token == null || token.isBlank()) {
        System.out.println("PUSH NO ENVIADA: token vacío");
        return;
    }

    try {
        System.out.println("ENVIANDO PUSH A TOKEN: " + token);

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        String response = FirebaseMessaging.getInstance().send(message);

        System.out.println("PUSH ENVIADA OK: " + response);

    } catch (FirebaseMessagingException e) {
        System.out.println("ERROR FIREBASE PUSH: " + e.getMessagingErrorCode());
        System.out.println("DETALLE: " + e.getMessage());
        e.printStackTrace();
    }
}
}