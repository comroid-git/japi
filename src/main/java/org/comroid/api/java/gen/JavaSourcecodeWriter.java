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
import java.util.Optional;
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
        write("import");
        write(type);
        write(';');
        lf();
        imports.add(type);
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
            var onlyValue = attributes.size() == 1 && attributes.containsKey("value");
            writeTokenList("(", ")", attributes.entrySet(), attr -> onlyValue ? attr.getValue() : "%s = %s".formatted(attr.getKey(), attr.getValue()), ",");
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
            @Singular(ignoreNullCollections = true) List<Object> implementsTypes
    ) throws IOException {
        if (name.isBlank())
            throw new IllegalArgumentException("Class name cannot be empty");
        if (kind == null) kind = CLASS;
        if (Stream.of(CLASS, ElementKind.INTERFACE, ENUM, RECORD, ANNOTATION_TYPE).noneMatch(kind::equals))
            throw new IllegalArgumentException("Invalid class kind: " + kind.name());
        writeIndent();
        writeModifiers(modifiers);
        writeWhitespaced(kind == ANNOTATION_TYPE ? "@interface" : kind.name().toLowerCase());
        writeWhitespaced(name);
        if (extendsType != null) {
            writeWhitespaced("extends");
            write(extendsType);
        }
        writeTokenList(kind == ElementKind.INTERFACE ? "extends" : "implements", "", implementsTypes, this::getImportedOrCanonicalClassName, ",");
        beginBlock(kind, name);
        return this;
    }

    public JavaSourcecodeWriter writeGetter(
            @Nullable @MagicConstant(flagsFromClass = Modifier.class) Integer modifiers,
            @NotNull Class<?> returnType,
            @NotNull @Language(value = "Java", prefix = "class $ {void ", suffix = " = null;}") String propertyName
    ) throws IOException {
        if (propertyName.isBlank())
            throw new IllegalArgumentException("Name cannot be empty");
        return writeGetter(modifiers, returnType, propertyName,
                "get" + Character.toUpperCase(propertyName.charAt(0)) + (propertyName.length() < 2 ? "" : propertyName.substring(1)));
    }

    public JavaSourcecodeWriter writeGetter(
            @Nullable @MagicConstant(flagsFromClass = Modifier.class) Integer modifiers,
            @NotNull Class<?> returnType,
            @Language(value = "Java", prefix = "var x = ", suffix = ";") String expression,
            @NotNull @Language(value = "Java", prefix = "class $ {void ", suffix = "(){}}") String methodName
    ) throws IOException {
        if (expression.isBlank() || methodName.isBlank())
            throw new IllegalArgumentException("Name cannot be empty");
        return writeMethodHeader(modifiers, returnType, methodName, List.of(), List.of())
                .writeIndent()
                .writeStatement("return " + expression)
                .end();
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
        if (modifiers == null)
            modifiers = 0;
        writeIndent();
        writeModifiers(modifiers);
        ElementKind kind;
        if (name.endsWith("ctor"))
            writeWhitespaced(currentContext(CLASS).map(CodeContext::getElementName).orElseThrow());
        else {
            write(returnType);
            writeWhitespaced(name);
        }
        write('(');
        writeTokenList("", "", parameters, Parameter::toString, ",");
        write(')');
        writeTokenList("throws", "", throwsTypes, this::getImportedOrCanonicalClassName, ",");
        if (Bitmask.isFlagSet(modifiers, ABSTRACT))
            writeLineTerminator();
        else beginBlock("ctor".equals(name) ? CONSTRUCTOR : METHOD, name);
        return this;
    }

    public JavaSourcecodeWriter beginBlock(ElementKind kind, String name) throws IOException {
        writeWhitespaced("{");
        lf();
        contexts.push(new CodeContext(kind, name, ++indentLevel, "}\n", true));
        return this;
    }

    @Builder(builderClassName = "BeginEnumConstant", builderMethodName = "beginEnumConstant", buildMethodName = "and")
    public JavaSourcecodeWriter writeEnumConstant(
            @NotNull @Language(value = "Java", prefix = "enum x { ", suffix = "; }") String name,
            @Singular(ignoreNullCollections = true) List<String> arguments
    ) throws IOException {
        if (name.isBlank())
            throw new IllegalArgumentException("Package name cannot be empty");
        writeIndent();
        write(name);
        if (!arguments.isEmpty())
            writeTokenList("(", ")", arguments, Function.identity(), ",");
        return this;
    }

    @Builder(builderClassName = "BeginField", builderMethodName = "beginField", buildMethodName = "and")
    public JavaSourcecodeWriter writeFieldHeader(
            @Nullable @MagicConstant(flagsFromClass = Modifier.class) Integer modifiers,
            @NotNull Object type,
            @NotNull @Language(value = "Java", prefix = "class $ {void ", suffix = " = null;}") String name
    ) throws IOException {
        if (name.isBlank())
            throw new IllegalArgumentException("Package name cannot be empty");
        writeIndent();
        writeModifiers(modifiers);
        if (type instanceof Class<?> cls)
            write(cls);
        else writeWhitespaced(String.valueOf(type));
        writeWhitespaced(name);
        contexts.push(new CodeContext(FIELD, name, ";\n"));
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

    public JavaSourcecodeWriter writeStatement(@NotNull @Language(value = "Java", prefix = "void x() { ", suffix = " }") String stmt) throws IOException {
        write(stmt);
        if (!stmt.endsWith(";"))
            writeLineTerminator();
        else lf();
        return this;
    }

    public JavaSourcecodeWriter writeExpression(@Nullable @Language(value = "Java", prefix = "var x = ", suffix = ";") String expr) throws IOException {
        writeWhitespaced(expr == null ? "null" : expr);
        return this;
    }

    @Contract("-> this")
    public JavaSourcecodeWriter end() throws IOException {
        contexts.pop().terminate();
        indentLevel = currentContext().map(CodeContext::getIndentLevel).orElse(0);
        flush();
        return this;
    }

    @SuppressWarnings("resource")
    public JavaSourcecodeWriter endAll() throws IOException {
        while (!contexts.isEmpty())
            end();
        return this;
    }

    public JavaSourcecodeWriter write(@Language(value = "Java", prefix = "void x() { ", suffix = " }") String... lines) throws IOException {
        for (var line : List.of(lines)) {
            writeIndent();
            write(line);
            lf();
        }
        return this;
    }

    @Override
    public void write(char @NotNull [] buf, int off, int len) throws IOException {
        length += len;
        super.write(buf, off, len);
        if (buf.length > 0) {
            var last = off + len - 1;
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
        } else writeWhitespaced(getImportedOrCanonicalClassName(type));
    }

    private void writeModifiers(@Nullable @MagicConstant(flagsFromClass = Modifier.class) Integer modifiers) throws IOException {
        if (modifiers == null) modifiers = 0;
        for (var entry : Map.<@NotNull Integer, String>of(
                PUBLIC, "public",
                PROTECTED, "protected",
                PRIVATE, "private"
        ).entrySet()) {
            if (currentContext().map(CodeContext::getKind).filter(k -> k == ElementKind.INTERFACE && entry.getKey() == PUBLIC).isPresent())
                continue;
            if (Bitmask.isFlagSet(modifiers, entry.getKey())) {
                writeWhitespaced(entry.getValue());
                break;
            }
        }
        for (var entry : Map.<@NotNull Integer, String>of(
                ABSTRACT, "abstract",
                STATIC, "static",
                FINAL, "final",
                SYNCHRONIZED, "synchronized",
                VOLATILE, "volatile",
                TRANSIENT, "transient",
                NATIVE, "native"
        ).entrySet()) {
            if (currentContext().map(CodeContext::getKind).filter(k -> k == ElementKind.INTERFACE && entry.getKey() == ABSTRACT).isPresent())
                continue;
            if (Bitmask.isFlagSet(modifiers, entry.getKey()))
                writeWhitespaced(entry.getValue());
        }
    }

    public <T> JavaSourcecodeWriter writeTokenList(String prefix, String suffix, Collection<T> source, Function<T, String> toString, String separator)
    throws IOException {
        var iter = source.iterator();
        if (!iter.hasNext())
            return this;
        writeWhitespaced(prefix);
        while (iter.hasNext()) {
            writeWhitespaced(toString.apply(iter.next()));
            if (iter.hasNext()) write(separator);
        }
        writeWhitespaced(suffix);
        return this;
    }

    public JavaSourcecodeWriter comma() throws IOException {
        write(',');
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

    public void ws() throws IOException {
        if (!whitespaced)
            write(' ');
    }

    public void lf() throws IOException {
        if (!newline)
            write('\n');
    }

    private String getImportedOrCanonicalClassName(Object obj) {
        if (obj instanceof String str)
            return str;
        var type   = (Class<?>) obj;
        var target = type.isArray() ? type.getComponentType() : type;
        if ("java.lang".equals(target.getPackageName()))
            return type.getSimpleName();
        if (imports.stream().map(Class::getCanonicalName).anyMatch(target.getCanonicalName()::equals))
            return type.getSimpleName();
        else return type.getCanonicalName();
    }

    private Optional<CodeContext> currentContext() {
        return contexts.isEmpty() ? Optional.empty() : Optional.ofNullable(contexts.peek());
    }

    private Optional<CodeContext> currentContext(ElementKind kind) {
        return contexts.reversed().stream()
                .filter(ctx -> ctx.kind == kind)
                .findFirst();
    }

    @Value
    private class CodeContext implements Terminatable {
        ElementKind kind;
        String      elementName;
        int         indentLevel;
        boolean     indentTerminator;
        @Nullable String terminator;

        public CodeContext(ElementKind kind, String elementName) {
            this(kind, elementName, 0);
        }

        public CodeContext(ElementKind kind, String elementName, int indentLevel) {
            this(kind, elementName, indentLevel, null);
        }

        public CodeContext(ElementKind kind, String elementName, @Nullable String terminator) {
            this(kind, elementName, 0, terminator, false);
        }

        public CodeContext(ElementKind kind, String elementName, int indentLevel, @Nullable String terminator) {
            this(kind, elementName, indentLevel, terminator, false);
        }

        public CodeContext(ElementKind kind, String elementName, int indentLevel, @Nullable String terminator, boolean indentTerminator) {
            this.kind             = kind;
            this.elementName      = elementName;
            this.indentLevel      = indentLevel;
            this.indentTerminator = indentTerminator;
            this.terminator       = terminator;
        }

        public void terminate() throws IOException {
            if (terminator == null) return;
            if (indentTerminator) writeIndent(indentLevel - 1);
            write(terminator);
        }
    }

    @Value
    public class Parameter {
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
            return "%s%s %s".formatted(getImportedOrCanonicalClassName(type), varargs ? "..." : "", name);
        }
    }
}
