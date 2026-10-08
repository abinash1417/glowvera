package com.glowvera.web;

import com.glowvera.dto.response.AnalyticsViews;
import com.glowvera.service.AnalyticsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analytics;

    public AdminAnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping
    public ApiResponse<AnalyticsViews.Summary> summary(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return ApiResponse.ok(analytics.summary(days));
    }
}
