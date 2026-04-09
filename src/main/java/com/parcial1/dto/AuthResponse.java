package com.parcial1.dto;

import com.parcial1.model.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponse {
    private String token;
    private String id;
    private String name;
    private String email;
    private Role role;
}