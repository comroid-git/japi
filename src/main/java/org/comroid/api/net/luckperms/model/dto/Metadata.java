package org.comroid.api.net.luckperms.model.dto;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record Metadata(
        @Nullable Map<String, String> meta,
        @Nullable String prefix,
        @Nullable String suffix,
        @Nullable String primaryGroup
) {}
