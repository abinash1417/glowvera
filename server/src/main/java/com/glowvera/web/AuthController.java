package com.glowvera.web;

import com.glowvera.dto.request.LoginRequest;
import com.glowvera.dto.request.RegisterRequest;
import com.glowvera.dto.response.UserView;
import com.glowvera.security.AuthCookieFactory;
import com.glowvera.security.AuthenticatedUser;
import com.glowvera.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final AuthCookieFactory cookies;

    public AuthController(AuthService auth, AuthCookieFactory cookies) {
        this.auth = auth;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, UserView>>> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = auth.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookies.create(result.token()).toString())
                .body(ApiResponse.ok(Map.of("user", result.user())));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, UserView>>> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = auth.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.create(result.token()).toString())
                .body(ApiResponse.ok(Map.of("user", result.user())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .body(ApiResponse.ok(Map.of("message", "Logged out")));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, UserView>> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(Map.of("user", user.toView()));
    }
}
