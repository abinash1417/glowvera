package com.glowvera.service;

import com.glowvera.common.AppException;
import com.glowvera.common.ErrorCode;
import com.glowvera.domain.Role;
import com.glowvera.dto.request.LoginRequest;
import com.glowvera.dto.request.RegisterRequest;
import com.glowvera.dto.response.UserView;
import com.glowvera.entity.User;
import com.glowvera.repository.UserRepository;
import com.glowvera.security.AuthenticatedUser;
import com.glowvera.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    // The user to show plus the signed token to put in the cookie. //
    public record AuthResult(UserView user, String token) {
    }

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.dummyHash = encoder.encode("timing-protection-dummy");
    }

    public AuthResult register(RegisterRequest request) {
        if (users.findByEmail(request.email()).isPresent()) {
            throw AppException.conflict("An account with this email already exists");
        }
        // Customers can only ever register as CUSTOMER. Admins are created by the server operator.
        User user = users.save(new User(request.name(), request.email(), request.phone(),
                encoder.encode(request.password()), Role.CUSTOMER));
        return new AuthResult(AuthenticatedUser.from(user).toView(), jwt.issue(user));
    }

    public AuthResult login(LoginRequest request) {
        User user = users.findByEmail(request.email()).orElse(null);
        boolean ok = encoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);

        // One message for both failures: attackers cannot learn which emails are registered.
        if (user == null || !ok) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }
        return new AuthResult(AuthenticatedUser.from(user).toView(), jwt.issue(user));
    }
}
