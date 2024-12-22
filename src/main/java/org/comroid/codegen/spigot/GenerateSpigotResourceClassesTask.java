package org.comroid.codegen.spigot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.Described;
import org.comroid.api.attr.Named;
import org.comroid.api.java.gen.JavaSourcecodeWriter;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import javax.lang.model.element.ElementKind;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.*;
import static javax.lang.model.element.ElementKind.*;

public abstract class GenerateSpigotResourceClassesTask extends DefaultTask {
    private final Set<String> terminalNodes = new HashSet<>();

    @InputDirectory
    public File getPluginResourcesDirectory() {
        return getProject().getExtensions().getByType(SourceSetContainer.class)
                .getByName("main").getResources()
                .getSrcDirs().iterator().next();
    }

    @OutputDirectory
    public File getGeneratedSourceCodeDirectory() {
        return new File(getProject().getLayout().getBuildDirectory().getAsFile().get(), "generated/sources/r/");
    }

    @TaskAction
    public void generate() {
        try (var pluginYml = new FileInputStream(new File(getPluginResourcesDirectory(), "plugin.yml"))) {
            Map<String, Object> yml    = new Yaml().load(pluginYml);
            var                 main   = (String) yml.get("main");
            var                 lio    = main.lastIndexOf('.');
            var                 pkg    = main.substring(0, lio);
            var                 pkgDir = new File(getGeneratedSourceCodeDirectory(), pkg.replace('.', '/'));
            if (!pkgDir.exists() && !pkgDir.mkdirs())
                throw new RuntimeException("Unable to create output directory");
            try (
                    var sourcecode = new FileWriter(new File(pkgDir, "PluginYml.java"));
                    var java = new JavaSourcecodeWriter(sourcecode)
            ) {
                java.writePackage(pkg)
                        .writeImport(Named.class, Described.class, Getter.class, RequiredArgsConstructor.class, SuppressWarnings.class)
                        .writeAnnotation(SuppressWarnings.class, Map.of("value", "unused"))
                        .beginClass().modifiers(PUBLIC).kind(ElementKind.INTERFACE).name("PluginYml").and();
                //.beginMethod().modifiers(PRIVATE).name("ctor").and().end();

                generateBaseFields(java, yml);
                generateCommandsEnum(java, Polyfill.uncheckedCast(yml.getOrDefault("commands", Map.of())));
                generatePermissions(java, Polyfill.uncheckedCast(yml.getOrDefault("permissions", new HashMap<>())));

                java.beginClass().kind(ENUM).name("LoadTime").and()
                        .writeIndent()
                        .writeTokenList("", "", List.of("STARTUP", "POSTWORLD"), Function.identity(), ",")
                        .writeLineTerminator()
                        .end();
            }
        } catch (FileNotFoundException | SecurityException e) {
            throw new RuntimeException("Unable to read plugin.yml", e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to generate plugin.yml resource class", e);
        }
    }

    private void generateBaseFields(JavaSourcecodeWriter java, Map<String, Object> yml) throws IOException {
        generateField(java, 0, String.class, "mainClassName", fromString(yml, "main"), toStringExpr());
        generateField(java, 0, String.class, "loggingPrefix", fromString(yml, "prefix"), toStringExpr());
        generateField(java, 0, String.class, "apiVersion", fromString(yml, "api-version"), toStringExpr());
        generateField(java, 0, "LoadTime", "load", fromString(yml, "load"), toEnumConstExpr("LoadTime"));
        generateField(java, 0, String.class, "name", fromString(yml, "name"), toStringExpr());
        generateField(java, 0, String.class, "version", fromString(yml, "version"), toStringExpr());
        generateField(java, 0, String.class, "description", fromString(yml, "description"), toStringExpr());
        generateField(java, 0, String.class, "website", fromString(yml, "website"), toStringExpr());
        generateField(java, 0, String.class, "author", fromString(yml, "author"), toStringExpr());
        generateField(java, 0, String[].class, "authors", fromStringList(yml, "authors"), toStringArrayExpr());
        generateField(java, 0, String[].class, "depend", fromStringList(yml, "depend"), toStringArrayExpr());
        generateField(java, 0, String[].class, "softdepend", fromStringList(yml, "softdepend"), toStringArrayExpr());
        generateField(java, 0, String[].class, "loadbefore", fromStringList(yml, "loadbefore"), toStringArrayExpr());
        generateField(java, 0, String[].class, "libraries", fromStringList(yml, "libraries"), toStringArrayExpr());
    }

    private void generateCommandsEnum(JavaSourcecodeWriter java, Map<String, Object> commands) throws IOException {
        java
                .beginAnnotation().type(Getter.class).and()
                .beginAnnotation().type(RequiredArgsConstructor.class).and()
                .beginClass().kind(ENUM).name("Command")
                .implementsType(Named.class).implementsType(Described.class).and();

        var iter = Polyfill.<Map<String, Map<String, Object>>>uncheckedCast(commands).entrySet().iterator();
        while (iter.hasNext()) {
            var each = iter.next();
            java.beginEnumConstant().name(each.getKey().toUpperCase())
                    .argument(toStringExpr().apply(fromString(each.getValue(), "description").get()))
                    .argument(toStringArrayExpr().apply(fromStringList(each.getValue(), "aliases").get()))
                    .argument(toStringExpr().apply(fromString(each.getValue(), "permission").get()))
                    .argument(toStringExpr().apply(fromString(each.getValue(), "permission-message").get()))
                    .argument(toStringExpr().apply(fromString(each.getValue(), "usage").get()))
                    .and();
            if (iter.hasNext())
                java.comma().lf();
        }
        java.writeLineTerminator().lf();

        generateField(java, PRIVATE | FINAL, String.class, "description");
        generateField(java, PRIVATE | FINAL, String[].class, "aliases");
        generateField(java, PRIVATE | FINAL, String.class, "requiredPermission");
        generateField(java, PRIVATE | FINAL, String.class, "permissionMessage");
        generateField(java, PRIVATE | FINAL, String.class, "usage");

        java.writeGetter(PUBLIC, String.class, "getName()", "toString");

        java.end();
    }

    private void cleanupKeys(Map<String, Map<String, Object>> data) {cleanupKeys(data, "", 0);}

    private void cleanupKeys(Map<String, Map<String, Object>> permissions, String compoundKey, int depth) {
        for (var permissionKey : permissions.keySet().toArray(String[]::new)) {
            // only when path blob count differs from depth
            var count = permissionKey.chars().filter(x->x=='.').count();
            if (count == depth) continue;
            if (count < depth) throw new IllegalArgumentException("Inner permissionKey does not have enough path blobs: " + permissionKey);

            // wrap own permissions
            var children = new HashMap<String, Map<String, Object>>();
            var split    = permissionKey.split("\\.");
            var wrap     = permissions.getOrDefault(permissionKey, Map.of());
            if (split.length > 1) {
                for (var c = split.length; c > 1; c--) {
                    var intermediateKey = compoundKey;
                    for (var i = 0; i < c; i++)
                        intermediateKey += '.' + split[i];
                    if (intermediateKey.startsWith("."))
                        intermediateKey = intermediateKey.substring(1);
                    children.put(intermediateKey, wrap);
                    wrap = new HashMap<>() {{put("children", children);}};
                }

                // replace old stuff
                permissions.merge(split[0], wrap, this::mergeMapsRec);
                permissions.remove(permissionKey);
            }

            // recurse into children
            var ck = (compoundKey.isBlank() ? "" : compoundKey + '.') + permissionKey;
            cleanupKeys(children, ck, (int) ck.chars().filter(x -> x == '.').count());
        }
    }

    private Map<String, Object> mergeMapsRec(Map<String, Object> a, Map<String, Object> b) {
        b.forEach((k1, v1) -> a.compute(k1, (k0, v0) -> {
            if (v0 instanceof Map<?, ?> inner0 && v1 instanceof Map<?, ?> inner1)
                return mergeMapsRec(Polyfill.uncheckedCast(inner0), Polyfill.uncheckedCast(inner1));
            return v1;
        }));
        return a;
    }

    private void generatePermissions(JavaSourcecodeWriter java, Map<String, Map<String, Object>> permissions) throws IOException {
        java.beginClass().kind(ElementKind.INTERFACE).name("Permission")
                .implementsType(Named.class).implementsType(Described.class).and()
                .beginMethod().modifiers(ABSTRACT).returnType(boolean.class).name("getDefaultValue").and();

        terminalNodes.clear();
        cleanupKeys(permissions);
        generatePermissionsNodes(java, "#", Polyfill.uncheckedCast(permissions));

        generateField(java, 0, "Permission[]", "TERMINAL_NODES", () -> terminalNodes,
                ls -> ls.isEmpty() ? "new Permission[0]" : ls.stream().collect(Collectors.joining(",", "new Permission[]{", "}")));
        java.end();
    }

    private void generatePermissionsNodes(JavaSourcecodeWriter java, String parentKey, Map<String, Object> nodes) throws IOException {
        for (var key : nodes.keySet()) {
            var name = key.startsWith(parentKey) ? key.substring(parentKey.length() + 1) : key;
            generatePermissionsNode(java, parentKey, name, Polyfill.uncheckedCast(nodes.get(key)));
        }
    }

    private void generatePermissionsNode(JavaSourcecodeWriter java, String parentKey, String key, Map<String, Object> node) throws IOException {
        var name = key.startsWith(parentKey) ? key.substring(parentKey.length() + 1) : key;
        java
                .beginAnnotation().type(Getter.class).and()
                .beginAnnotation().type(RequiredArgsConstructor.class).and()
                .beginClass().modifiers(PUBLIC).kind(ENUM).name(name).implementsType("Permission").and();

        generatePermissionEnumConstant("$self", java, key, node);
        java.comma().lf();
        generatePermissionEnumConstant("$wildcard", java, key + ".*", node);

        var deepChildren = new HashMap<String, Object>();
        var children     = Polyfill.<Map<String, Object>>uncheckedCast(node.getOrDefault("children", Map.of()));
        if (!children.isEmpty())
            for (var entry : children.entrySet()) {
                var midKey      = "#".equals(parentKey) ? key : parentKey + '.' + key;
                var eKey        = entry.getKey();
                var eName       = eKey.startsWith(midKey) ? eKey.substring(midKey.length() + 1) : eKey;
                var subChildren = Polyfill.<Map<String, Object>>uncheckedCast(entry.getValue()).getOrDefault("children", Map.of());
                if (Polyfill.<Map<String, Object>>uncheckedCast(subChildren).isEmpty()) {
                    // no further children; write enum constant
                    java.comma().lf();
                    terminalNodes.add(eKey);
                    generatePermissionEnumConstant(eName, java, eKey, Polyfill.uncheckedCast(entry.getValue()));
                } else deepChildren.put(eKey, entry.getValue());
            }
        java.writeLineTerminator().lf();

        generateField(java, PRIVATE | FINAL, String.class, "name");
        generateField(java, PRIVATE | FINAL, String.class, "description");
        generateField(java, PRIVATE | FINAL, boolean.class, "defaultValue");

        java.writeGetter(PUBLIC, String.class, "name", "toString")
                .writeGetter(PUBLIC, boolean.class, "defaultValue");

        generatePermissionsNodes(java, key, deepChildren);
        java.end();
    }

    private void generatePermissionEnumConstant(
            @NotNull @Language(value = "Java", prefix = "enum x { ", suffix = "; }") String name,
            JavaSourcecodeWriter java,
            String key,
            Map<String, Object> node
    ) throws IOException {
        java.writeEnumConstant(name, List.of(
                toStringExpr().apply(key),
                toStringExpr().apply(fromString(node, "description").get()),
                toPlainExpr().apply(fromBoolean(node, "default").get())
        ));
    }

    private <T> void generateField(
            JavaSourcecodeWriter java,
            @SuppressWarnings("SameParameterValue") @MagicConstant(flagsFromClass = Modifier.class) Integer modifiers,
            Class<?> type,
            @Language(value = "Java", prefix = "var ", suffix = " = null") String name
    ) throws IOException {generateField(java, modifiers, type, name, null, null);}

    private <T> void generateField(
            JavaSourcecodeWriter java,
            @MagicConstant(flagsFromClass = Modifier.class) Integer modifiers,
            Object type,
            @Language(value = "Java", prefix = "var ", suffix = " = null") String name,
            @Nullable Supplier<@Nullable T> source,
            @Nullable Function<T, String> toExpr
    ) throws IOException {
        java.writeFieldHeader(modifiers, type, name);
        if (source != null && toExpr != null) {
            var value = source.get();
            java.writeDeclaration().writeExpression(value == null ? "null" : toExpr.apply(value));
        }
        java.end();
    }

    private static Supplier<@NotNull Boolean> fromBoolean(Map<String, Object> data, String key) {
        return () -> (boolean) data.getOrDefault(key, false);
    }

    private static Supplier<String> fromString(Map<String, Object> data, String key) {
        return () -> (String) data.getOrDefault(key, null);
    }

    private static Supplier<List<String>> fromStringList(Map<String, Object> data, String key) {
        return () -> {
            var value = data.get(key);
            if (value == null)
                return List.of();
            if (value instanceof List ls)
                return ls;
            return List.of((String) value);
        };
    }

    private static <T> Function<T, String> toPlainExpr() {return String::valueOf;}

    private static Function<String, String> toStringExpr() {return args -> args == null || "null".equals(args) ? "null" : "\"%s\"".formatted(args);}

    private static Function<List<String>, String> toStringArrayExpr() {
        return ls -> ls.isEmpty() ? "new String[0]" : "new String[]{%s}".formatted(ls.stream()
                .collect(Collectors.joining("\",\"", "\"", "\"")));
    }

    @SuppressWarnings("SameParameterValue")
    private static Function<String, String> toEnumConstExpr(String enumTypeName) {
        return v -> "%s.%s".formatted(enumTypeName, v);
    }
}
