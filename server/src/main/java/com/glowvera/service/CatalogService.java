package com.glowvera.service;

import com.glowvera.dto.response.FiltersResponse;
import com.glowvera.dto.response.Refs;
import com.glowvera.repository.CategoryRepository;
import com.glowvera.repository.ConcernRepository;
import com.glowvera.repository.SkinTypeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private static final Sort BY_NAME = Sort.by("name");

    private final CategoryRepository categories;
    private final SkinTypeRepository skinTypes;
    private final ConcernRepository concerns;

    public CatalogService(CategoryRepository categories, SkinTypeRepository skinTypes, ConcernRepository concerns) {
        this.categories = categories;
        this.skinTypes = skinTypes;
        this.concerns = concerns;
    }

    public FiltersResponse getFilters() {
        return new FiltersResponse(
                categories.findAll(BY_NAME).stream()
                        .map(c -> new Refs.CategoryRef(c.getId(), c.getName(), c.getSlug())).toList(),
                skinTypes.findAll(BY_NAME).stream().map(s -> new Refs.NameRef(s.getId(), s.getName())).toList(),
                concerns.findAll(BY_NAME).stream().map(c -> new Refs.NameRef(c.getId(), c.getName())).toList());
    }
}
