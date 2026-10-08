package com.glowvera.dto.response;

// Small id+name references used inside other responses. //
public final class Refs {

    private Refs() {
    }

    public record CategoryRef(Long id, String name, String slug) {
    }

    public record NameRef(Long id, String name) {
    }

    public record PublicCategory(String name, String slug) {
    }
}
