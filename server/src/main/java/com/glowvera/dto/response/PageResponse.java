package com.glowvera.dto.response;

import java.util.List;

public record PageResponse<T>(List<T> items, Pagination pagination) {
}
