package com.glowvera.dto.response;

import com.glowvera.domain.Role;

public record UserView(Long id, String name, String email, String phone, Role role) {
}
