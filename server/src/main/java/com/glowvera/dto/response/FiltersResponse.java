package com.glowvera.dto.response;

import java.util.List;

public record FiltersResponse(
        List<Refs.CategoryRef> categories,
        List<Refs.NameRef> skinTypes,
        List<Refs.NameRef> concerns) {
}
