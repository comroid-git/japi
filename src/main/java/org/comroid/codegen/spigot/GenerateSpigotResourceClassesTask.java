package org.comroid.codegen.spigot;

import org.comroid.api.Polyfill;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.*;

public abstract class GenerateSpigotResourceClassesTask extends DefaultTask {
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
        try (
                var pluginYml = new FileInputStream(new File(getPluginResourcesDirectory(), "plugin.yml"));
                var sourcecode = new FileWriter(new File(getGeneratedSourceCodeDirectory(), "PluginYml.java"));
                var java = new JavaSourcecodeWriter(sourcecode)
        ) {
            Map<String, Object> yml = new Yaml().load(pluginYml);

            var main = (String) yml.get("main");
            var lio  = main.lastIndexOf('.');
            java.writePackage(main.substring(0, lio))
                    .beginClass().modifiers(PUBLIC).kind(ElementKind.INTERFACE).name("PluginYml").and();
            //.beginMethod().modifiers(PRIVATE).name("ctor").and().end();

            generateBaseFields(java, yml);
            generateCommandsEnum(java, Polyfill.uncheckedCast(yml.getOrDefault("commands", Map.of())));
            generatePermissions(java, Polyfill.uncheckedCast(yml.getOrDefault("permissions", Map.of())));

            java.beginClass().kind(ElementKind.ENUM).name("LoadTime").and()
                    .writeIndent()
                    .writeTokenList("", "", List.of("STARTUP", "POSTWORLD"), Function.identity(), ",")
                    .writeLineTerminator()
                    .end();
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
        java.beginClass().kind(ElementKind.ENUM).name("Command").and();
        var iter = Polyfill.<Map<String, Map<String, Object>>>uncheckedCast(commands).entrySet().iterator();
        while (iter.hasNext()) {
            var each = iter.next();
            java.beginEnumConstant().name(each.getKey())
                    .argument(toStringExpr().apply(fromString(each.getValue(), "description").get()))
                    .argument(toStringExpr().apply(fromString(each.getValue(), "permission").get()))
                    .argument(toStringArrayExpr().apply(fromStringList(each.getValue(), "aliases").get()))
                    .and();
            if (iter.hasNext())
                java.comma().lf();
        }
        java.writeLineTerminator().lf();
        generateField(java, PUBLIC | FINAL, String.class, "description");
        generateField(java, PUBLIC | FINAL, String.class, "requiredPermission");
        generateField(java, PUBLIC | FINAL, String[].class, "aliases");
        java.beginMethod().name("ctor")
                .parameter(java.new Parameter(String.class, "description"))
                .parameter(java.new Parameter(String.class, "requiredPermission"))
                .parameter(java.new Parameter(String[].class, "aliases", true))
                .and()
                .write(
                        "this.description = description;",
                        "this.requiredPermission = requiredPermission;",
                        "this.aliases = aliases;")
                .end();
        java.end();
    }

    private void generatePermissions(JavaSourcecodeWriter java, Map<String, Object> permissions) throws IOException {
        java.beginClass().kind(ElementKind.INTERFACE).name("Permission").and();
        generatePermissionsNodes(java, "#", permissions);
        java.end();
    }

    private void generatePermissionsNodes(JavaSourcecodeWriter java, String parentKey, Map<String, Object> nodes) throws IOException {
        for (var name : nodes.keySet())
            generatePermissionsNode(java, parentKey, name, Polyfill.uncheckedCast(nodes.get(name)));
    }

    private void generatePermissionsNode(JavaSourcecodeWriter java, String parentKey, String key, Map<String, Object> node) throws IOException {
        var name = key.startsWith(parentKey) ? key.substring(parentKey.length() + 1) : key;
        var id   = name.split("\\.");
        for (var idPart : id) {
            if (idPart.isBlank()) continue;
            java.beginClass().kind(ElementKind.INTERFACE).name(idPart).and();
        }

        generateField(java, 0, String.class, "permission", () -> key, toStringExpr());
        generateField(java, 0, String.class, "description", fromString(node, "description"), toStringExpr());
        generateField(java, 0, Boolean.class, "defaultValue", fromBoolean(node, "default"), toPlainExpr());
        var children = Polyfill.<Map<String, Object>>uncheckedCast(node.getOrDefault("children", Map.of()));
        generatePermissionsNodes(java, key, children);

        for (var $ : id)
            java.end();
    }

    private <T> void generateField(
            JavaSourcecodeWriter java,
            @SuppressWarnings("SameParameterValue") @MagicConstant(flagsFromClass = Modifier.class) Integer modifiers,
            Class<?> type,
            String name
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

    private static <T> Function<T, String> toPlainExpr() {return java.lang.String::valueOf;}

    private static Function<String, String> toStringExpr() {return "\"%s\""::formatted;}

    private static Function<List<String>, String> toStringArrayExpr() {
        return ls -> "new String[]{%s}".formatted(ls.stream()
                .collect(Collectors.joining("\",\"", "\"", "\"")));
    }

    @SuppressWarnings("SameParameterValue")
    private static Function<String, String> toEnumConstExpr(String enumTypeName) {
        return v -> "%s.%s".formatted(enumTypeName, v);
    }
}
