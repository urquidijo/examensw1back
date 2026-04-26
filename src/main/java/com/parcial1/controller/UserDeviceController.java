package com.parcial1.controller;

import com.parcial1.dto.FcmTokenRequest;
import com.parcial1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserDeviceController {

    private final UserRepository userRepository;

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> saveFcmToken(
            @RequestBody FcmTokenRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFcmToken(request.getFcmToken());
            userRepository.save(user);
        });

        return ResponseEntity.ok().build();
    }
}