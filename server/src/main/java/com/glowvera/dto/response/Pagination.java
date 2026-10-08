package com.glowvera.dto.response;

public record Pagination(int page, int limit, long total, int totalPages) {

    public static Pagination of(int page, int limit, long total) {
        return new Pagination(page, limit, total, (int) Math.ceil((double) total / limit));
    }
}
