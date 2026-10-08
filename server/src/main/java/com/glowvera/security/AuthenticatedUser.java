package com.glowvera.security;

import com.glowvera.domain.Role;
import com.glowvera.dto.response.UserView;
import com.glowvera.entity.User;

public record AuthenticatedUser(Long id, String name, String email, String phone, Role role) {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole());
    }

    public UserView toView() {
        return new UserView(id, name, email, phone, role);
    }
}
