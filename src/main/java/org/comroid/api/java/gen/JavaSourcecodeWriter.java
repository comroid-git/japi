package org.comroid.api.java.gen;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.comroid.api.func.util.Bitmask;
import org.comroid.api.io.WriterDelegate;
import org.comroid.api.tree.Terminatable;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ElementKind;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.*;
import static javax.lang.model.element.ElementKind.*;

@SuppressWarnings({ "resource", "unused", "UnusedReturnValue", "SameParameterValue" })
public class JavaSourcecodeWriter extends WriterDelegate {
    private final Set<Class<?>>      imports     = new HashSet<>();
    private final Stack<CodeContext> contexts    = new Stack<>();
    private       int                length      = 0;
    private       int                indentLevel = 0;
    private       boolean            whitespaced = false;
    private       boolean            newline     = false;

    public JavaSourcecodeWriter(Writer delegate) {
        super(delegate);
    }

    public JavaSourcecodeWriter writePackage(@NotNull @Language(value = "Java", prefix = "package ", suffix = ";") String name) throws IOException {
        if (length != 0)
            throw new IllegalStateException("Package declaration must be written at index 0");
        if (name.isBlank())
            throw new IllegalArgumentException("Package name cannot be empty");
        write("package");
        writeWhitespaced(name);
        write(';');
        lf();
        return this;
    }

    public JavaSourcecodeWriter writeImport(Class<?> @NotNull ... types) throws IOException {
        if (indentLevel > 0)
            throw new IllegalStateException("Imports must be written before class definition");
        if (types.length == 0)
            return this;
        Class<?> type;
        if (types.length > 1) {
            for (var each : types)
                writeImport(each);
            return this;
        } else type = types[0];
        imports.add(type);
        write("import");
        write(type);
        write(';');
        lf();
        return this;
    }

    @Builder(builderClassName = "BeginAnnotation", builderMethodName = "beginAnnotation", buildMethodName = "and")
    public JavaSourcecodeWriter writeAnnotation(
            @NotNull Class<? extends Annotation> type,
            @Singular(ignoreNullCollections = true) Map<String, String> attributes
    ) throws IOException {
        writeIndent();
        write('@');
        write(type);
        if (!attributes.isEmpty()) {
            write('(');
            if (attributes.size() == 1 && attributes.containsKey("value")) {
                writeExpression(attributes.get("value"));
            } else {
                writeTokenList("", attributes.entrySet(), attr -> "%s = %s".formatted(attr.getKey(), attr.getValue()), ",");
            }
            write(')');
        }
        lf();
        return this;
    }

    @Builder(builderClassName = "BeginClass", builderMethodName = "beginClass", buildMethodName = "and")
    public JavaSourcecodeWriter writeClassHeader(
            @Nullable @MagicConstant(flagsFromClass = Modifier.class) Integer modifiers,
            @Nullable ElementKind kind,
            @NotNull @Language(value = "Java", prefix = "class ", suffix = " {}") String name,
            @Nullable Class<?> extendsType,
            @Singular(ignoreNullCollections = true) List<Class<?>> implementsTypes
    ) throws IOException {
        if (name.isBlank())
            throw new IllegalArgumentException("Package name cannot be empty");
        if (kind == null) kind = CLASS;
        if (Stream.of(CLASS, ElementKind.INTERFACE, ENUM, RECORD, ANNOTATION_TYPE).noneMatch(kind::equals))
            throw new IllegalArgumentException("Invalid class kind: " + kind.name());
        writeIndent();
        writeModifiers(modifiers);
        writeWhitespaced("class");
        writeWhitespaced(name);
        if (extendsType != null) {
            writeWhitespaced("extends");
            write(extendsType);
        }
        writeTokenList("implements", implementsTypes, this::getImportedOrCanonicalClassName, ",");
        beginBlock(CLASS, name);
        return this;
    }

    @Builder(builderClassName = "BeginMethod", builderMethodName = "beginMethod", buildMethodName = "and")
    public JavaSourcecodeWriter writeMethodHeader(
            @Nullable @MagicConstant(flagsFromClass = Modifier.class) Integer modifiers,
            @NotNull Class<?> returnType,
            @NotNull @Language(value = "Java", prefix = "class $ {void ", suffix = "() {}}") String name,
            @Singular(ignoreNullCollections = true) List<Class<? extends Throwable>> throwsTypes,
            @Singular(ignoreNullCollections = true) List<Parameter> parameters
    ) throws IOException {
        if (name.isBlank())
            throw new IllegalArgumentException("Package name cannot be empty");
        writeIndent();
        writeModifiers(modifiers);
        ElementKind kind;
        if (name.endsWith("ctor"))
            writeWhitespaced(currentContext().elementName);
        else {
            write(returnType);
            writeWhitespaced(name);
        }
        write('(');
        writeTokenList("", parameters, Parameter::toString, ",");
        write(')');
        writeTokenList("throws", throwsTypes, this::getImportedOrCanonicalClassName, ",");
        beginBlock(METHOD, name);
        return this;
    }

    public JavaSourcecodeWriter beginBlock(ElementKind kind, String name) throws IOException {
        writeWhitespaced("{");
        lf();
        contexts.push(CodeContext.builder()
                .kind(kind)
                .elementName(name)
                .indentLevel(++indentLevel)
                .terminator("}\n")
                .indentTerminator(true)
                .build());
        return this;
    }

    @Builder(builderClassName = "BeginField", builderMethodName = "beginField", buildMethodName = "and")
    public JavaSourcecodeWriter writeFieldHeader(
            @Nullable @MagicConstant(flagsFromClass = Modifier.class) Integer modifiers,
            @NotNull Class<?> type,
            @NotNull @Language(value = "Java", prefix = "class $ {void ", suffix = " = null;}") String name
    ) throws IOException {
        if (name.isBlank())
            throw new IllegalArgumentException("Package name cannot be empty");
        writeIndent();
        writeModifiers(modifiers);
        write(type);
        writeWhitespaced(name);
        contexts.push(CodeContext.builder()
                .kind(FIELD)
                .elementName(name)
                .indentLevel(indentLevel)
                .terminator(";")
                .build());
        return this;
    }

    public JavaSourcecodeWriter writeDeclaration() throws IOException {
        writeWhitespaced("=");
        return this;
    }

    public JavaSourcecodeWriter writeLineTerminator() throws IOException {
        write(';');
        lf();
        return this;
    }

    public JavaSourcecodeWriter writeExpression(@Nullable @Language(value = "Java", prefix = "var x = ", suffix = ";") String expr) throws IOException {
        writeWhitespaced(expr == null ? "null" : expr);
        return this;
    }

    @Contract("-> this")
    public JavaSourcecodeWriter end() throws IOException {
        contexts.pop().terminate();
        return this;
    }

    @SuppressWarnings("resource")
    public JavaSourcecodeWriter endAll() throws IOException {
        while (!contexts.isEmpty())
            end();
        return this;
    }

    public JavaSourcecodeWriter write(String... lines) throws IOException {
        var iter = List.of(lines).iterator();
        if (iter.hasNext())
            write(iter.next());
        while (iter.hasNext()) {
            writeIndent();
            write(iter.next());
        }
        return this;
    }

    @Override
    public void write(char @NotNull [] buf, int off, int len) throws IOException {
        length += len;
        super.write(buf, off, len);
        if (buf.length > 0) {
            var last = off + len;
            if (0 <= last && last < buf.length) {
                newline     = buf[last] == '\n';
                whitespaced = newline || IntStream.of(' ', '\t').anyMatch(x -> buf[last] == x);
            }
        }
    }

    @Override
    public void close() throws IOException {
        endAll();
        super.close();
    }

    public JavaSourcecodeWriter writeIndent() throws IOException {
        writeIndent(indentLevel);
        return this;
    }

    private void write(@NotNull Class<?> type) throws IOException {
        if (type.isArray()) {
            write(type.getComponentType());
            write("[]");
        } else writeWhitespaced(getImportedOrCanonicalClassName(type));
    }

    private void writeModifiers(@Nullable @MagicConstant(flagsFromClass = Modifier.class) Integer modifiers) throws IOException {
        if (modifiers == null) modifiers = 0;
        for (var entry : Map.<@NotNull Integer, String>of(
                PUBLIC, "public",
                PROTECTED, "protected",
                PRIVATE, "private"
        ).entrySet())
            if (Bitmask.isFlagSet(modifiers, entry.getKey())) {
                writeWhitespaced(entry.getValue());
                break;
            }
        for (var entry : Map.<@NotNull Integer, String>of(
                STATIC, "static",
                FINAL, "final",
                SYNCHRONIZED, "synchronized",
                VOLATILE, "volatile",
                TRANSIENT, "transient",
                NATIVE, "native"
        ).entrySet())
            if (Bitmask.isFlagSet(modifiers, entry.getKey()))
                writeWhitespaced(entry.getValue());
    }

    public <T> JavaSourcecodeWriter writeTokenList(String key, Collection<T> source, Function<T, String> toString, String separator) throws IOException {
        var iter = source.iterator();
        if (!iter.hasNext())
            return this;
        writeWhitespaced(key);
        while (iter.hasNext()) {
            writeWhitespaced(toString.apply(iter.next()));
            if (iter.hasNext()) write(separator);
        }
        return this;
    }

    private void writeWhitespaced(String str) throws IOException {
        if (!whitespaced && !str.startsWith(" ")) ws();
        write(str);
    }

    private void writeIndented(String str) throws IOException {
        writeIndent(indentLevel);
        write(str);
    }

    private void writeIndent(int c) throws IOException {
        while (c-- > 0)
            write("    ");
    }

    private void ws() throws IOException {
        if (!whitespaced)
            write(' ');
    }

    private void lf() throws IOException {
        if (!newline)
            write('\n');
    }

    private String getImportedOrCanonicalClassName(Class<?> type) {
        if (imports.stream().map(Class::getCanonicalName).anyMatch(type.getCanonicalName()::equals))
            return type.getSimpleName();
        else return type.getCanonicalName();
    }

    private CodeContext currentContext() {
        return contexts.peek();
    }

    @Value
    @Builder(toBuilder = true)
    private class CodeContext implements Terminatable {
        ElementKind kind;
        String      elementName;
        @lombok.Builder.Default           int     indentLevel      = 0;
        @lombok.Builder.Default           boolean indentTerminator = false;
        @lombok.Builder.Default @Nullable String  terminator       = null;

        public void terminate() throws IOException {
            //noinspection ConstantValue
            if (terminator == null) return;
            if (indentTerminator) writeIndent(indentLevel);
            write(terminator);
        }
    }

    @Value
    public static class Parameter {
        Class<?> type;
        String   name;
        boolean  varargs;

        public Parameter(Class<?> type, String name) {this(type, name, false);}

        public Parameter(Class<?> type, String name, boolean varargs) {
            if (varargs && !type.isArray())
                throw new IllegalArgumentException("VarArgs parameter must be array type '%s[]'".formatted(type.getSimpleName()));
            this.type    = type;
            this.name    = name;
            this.varargs = varargs;
        }

        @Override
        public String toString() {
            return "%s%s %s".formatted(type.getCanonicalName(), varargs ? "..." : "", name);
        }
    }
}
