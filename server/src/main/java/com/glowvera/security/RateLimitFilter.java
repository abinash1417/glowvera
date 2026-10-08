package com.glowvera.security;

import com.glowvera.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String name, String method, String path, int limit, Duration window, boolean failuresOnly) {
    }

    private static final class Window {
        long startMillis;
        int count;
    }

    private static final List<Rule> RULES = List.of(
            new Rule("login", "POST", "/api/auth/login", 5, Duration.ofMinutes(15), true),
            new Rule("register", "POST", "/api/auth/register", 10, Duration.ofHours(1), false),
            new Rule("checkout", "POST", "/api/orders/checkout", 30, Duration.ofHours(1), false),
            new Rule("track", "GET", "/api/orders/track", 30, Duration.ofMinutes(15), false),
            new Rule("retry", "POST", "/api/payments/payhere/retry", 30, Duration.ofMinutes(15), false),
            new Rule("wa-link", "POST", "/api/whatsapp/link", 30, Duration.ofMinutes(15), false));

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ErrorResponseWriter writer;
    private final Clock clock;

    public RateLimitFilter(ErrorResponseWriter writer, Clock clock) {
        this.writer = writer;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<Rule> rule = RULES.stream()
                .filter(r -> r.method().equals(request.getMethod()) && r.path().equals(request.getRequestURI()))
                .findFirst();
        if (rule.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        Rule r = rule.get();
        String key = r.name() + ":" + request.getRemoteAddr();

        if (r.failuresOnly()) {
            if (currentCount(key, r) >= r.limit()) {
                reject(response, r);
                return;
            }
            chain.doFilter(request, response);
            if (response.getStatus() == 401) {
                increment(key, r);
            }
            return;
        }

        if (increment(key, r) > r.limit()) {
            reject(response, r);
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, Rule r) throws IOException {
        response.setHeader("Retry-After", String.valueOf(r.window().toSeconds()));
        writer.write(response, 429, ErrorCode.RATE_LIMITED.name(), "Too many attempts. Please try again later.");
    }

    private int currentCount(String key, Rule r) {
        Window w = windows.get(key);
        long now = clock.millis();
        return (w == null || now - w.startMillis >= r.window().toMillis()) ? 0 : w.count;
    }

    private int increment(String key, Rule r) {
        long now = clock.millis();
        int[] result = new int[1];
        windows.compute(key, (k, w) -> {
            if (w == null || now - w.startMillis >= r.window().toMillis()) {
                w = new Window();
                w.startMillis = now;
            }
            w.count++;
            result[0] = w.count;
            return w;
        });
        return result[0];
    }

    /** Drops finished windows so the map cannot grow forever. */
    @Scheduled(fixedDelay = 600_000)
    void cleanup() {
        long now = clock.millis();
        long longest = RULES.stream().mapToLong(r -> r.window().toMillis()).max().orElse(0);
        windows.entrySet().removeIf(e -> now - e.getValue().startMillis >= longest);
    }
}
