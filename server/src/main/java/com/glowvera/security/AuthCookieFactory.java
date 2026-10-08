package com.glowvera.security;

import com.glowvera.config.AppProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;


@Component
public class AuthCookieFactory {

    public static final String COOKIE_NAME = "glowvera_token";

    private final AppProperties props;
    private final JwtService jwt;

    public AuthCookieFactory(AppProperties props, JwtService jwt) {
        this.props = props;
        this.jwt = jwt;
    }

    public ResponseCookie create(String token) {
        return base(token).maxAge(jwt.lifetime()).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        boolean prod = props.production();
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(prod)
                .sameSite(prod ? "None" : "Lax")
                .path("/");
    }
}
