package org.comroid.api.text;

import lombok.SneakyThrows;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

@Value
public class Translation {
    public static final Map<String, Translation> LOADED = new ConcurrentHashMap<>();

    public static Translation get() {
        return get(Locale.getDefault());
    }

    public static Translation get(@NotNull Locale locale) {
        return get(ResourceBundle.getBundle("locale", locale, new LangFileResourceBundleControl()));
    }

    public static Translation get(@NotNull ResourceBundle strings) {
        return LOADED.computeIfAbsent(strings.getLocale().getLanguage(), k -> new Translation(strings.getLocale(), strings));
    }

    public static String str(@NotNull String key) {
        return get(Locale.getDefault()).get(key);
    }

    public static String str(@NotNull String key, @Nullable String fallback) {
        return get(Locale.getDefault()).get(key, fallback);
    }

    Locale         locale;
    ResourceBundle strings;

    public String get(@NotNull String key) {
        return wrapNewlines(strings.getString(key));
    }

    public String get(@NotNull String key, @Nullable String fallback) {
        return wrapNewlines(strings.containsKey(key) ? strings.getString(key) : Objects.requireNonNullElse(fallback, key));
    }

    private static String wrapNewlines(String raw) {
        return raw.replaceAll("[\\\\%]r[\\\\%]n", "\n");
    }

    @Value
    private static class LangFileResourceBundle extends ResourceBundle {
        Properties strings;

        @SneakyThrows
        public LangFileResourceBundle(InputStream data) {
            strings = new Properties() {{
                load(new InputStreamReader(data));
            }};
        }

        @Override
        protected String handleGetObject(@NotNull String key) {
            return String.valueOf(strings.getOrDefault(key, key));
        }

        @Override
        public @NotNull Enumeration<String> getKeys() {
            return Collections.enumeration(strings.stringPropertyNames());
        }
    }

    @Value
    private static class LangFileResourceBundleControl extends ResourceBundle.Control {
        @Override
        public List<String> getFormats(String baseName) {
            return List.of("lang");
        }

        @Override
        @SneakyThrows
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) {
            if (!"lang".equals(format)) return null;

            var bundleName   = toBundleName(baseName, locale);
            var resourceName = toResourceName(bundleName, "lang").replace('_', '/');

            try (var data = loader.getResourceAsStream(resourceName)) {
                if (data != null) return new LangFileResourceBundle(data);
            }

            return null;
        }
    }
}
