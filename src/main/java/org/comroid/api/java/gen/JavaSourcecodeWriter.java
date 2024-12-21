package org.comroid.api.java.gen;

import lombok.Builder;
import lombok.Singular;
import org.comroid.api.io.WriterDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.lang.reflect.Modifier.*;

@SuppressWarnings("unused")
public class JavaSourcecodeWriter extends WriterDelegate {
    private final Stack<IndentedString> terminators = new Stack<>();
    private       int                   length      = 0;
    private       int                   indentLevel = 0;
    private       boolean               whitespaced = false;
    private       boolean               newline     = false;

    public JavaSourcecodeWriter(Writer delegate) {
        super(delegate);
    }

    public JavaSourcecodeWriter writePackage(@NotNull String name) throws IOException {
        if (length != 0)
            throw new IllegalStateException("Package declaration must be written at index 0");
        write("package");
        writeWhitespaced(name);
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
        write(type.getCanonicalName());
        if (!attributes.isEmpty()) {
            write('(');
            if (attributes.size() == 1 && attributes.containsKey("value")) {
                write(attributes.get("value"));
            } else {
                writeDeclarationList("", attributes.entrySet(), attr -> "%s = %s".formatted(attr.getKey(), attr.getValue()), ",");
            }
            write(')');
        }
        lf();
        return this;
    }

    @Builder(builderClassName = "BeginClass", builderMethodName = "beginClass", buildMethodName = "and")
    public JavaSourcecodeWriter writeClass(
            @Nullable @Singular(ignoreNullCollections = true) Set<@NotNull Integer> modifiers,
            @NotNull String name,
            @Nullable Class<?> extendsType,
            @Singular(ignoreNullCollections = true) List<Class<?>> implementsTypes
    ) throws IOException {
        writeIndent();
        writeModifiers(modifiers);
        writeWhitespaced("class");
        writeWhitespaced(name);
        if (extendsType != null) {
            writeWhitespaced("extends");
            writeWhitespaced(extendsType.getCanonicalName());
        }
        writeDeclarationList("implements", implementsTypes, Class::getCanonicalName, ",");
        beginBlock();
        return this;
    }

    @Builder(builderClassName = "BeginMethod", builderMethodName = "beginMethod", buildMethodName = "and")
    public JavaSourcecodeWriter writeMethod(
            @Nullable @Singular(ignoreNullCollections = true) Set<@NotNull Integer> modifiers,
            @NotNull Class<?> returnType,
            @NotNull String name,
            @Singular(ignoreNullCollections = true) List<Class<? extends Throwable>> throwsTypes,
            @Singular(ignoreNullCollections = true) List<Map.Entry<Class<?>, String>> parameters
    ) throws IOException {
        writeIndent();
        writeModifiers(modifiers);
        writeWhitespaced(returnType.getCanonicalName());
        writeWhitespaced(name);
        write('(');
        writeDeclarationList("", parameters, e -> "%s %s".formatted(e.getKey(), e.getValue()), ",");
        write(')');
        writeDeclarationList("throws", throwsTypes, Class::getCanonicalName, ",");
        beginBlock();
        return this;
    }

    public void beginBlock() throws IOException {
        writeWhitespaced("{");
        lf();
        terminators.push(new IndentedString("}\n", indentLevel++));
    }

    @Builder(builderClassName = "BeginField", builderMethodName = "beginField", buildMethodName = "and")
    public JavaSourcecodeWriter writeField(
            @Singular(ignoreNullCollections = true) Set<@NotNull Integer> modifiers,
            @NotNull Class<?> type,
            @NotNull String name
    ) throws IOException {
        writeIndent();
        writeModifiers(modifiers);
        writeWhitespaced(type.getCanonicalName());
        writeWhitespaced(name);
        return this;
    }

    public void beginExpression() throws IOException {
        writeWhitespaced("=");
        terminators.push(new IndentedString(";\n", 0));
    }

    @SuppressWarnings("UnusedReturnValue")
    public JavaSourcecodeWriter end() throws IOException {
        write(terminators.pop());
        return this;
    }

    @SuppressWarnings({ "UnusedReturnValue", "resource" })
    public JavaSourcecodeWriter endAll() throws IOException {
        while (!terminators.isEmpty())
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
                whitespaced = IntStream.of(' ', '\t').anyMatch(x -> buf[last] == x);
                newline     = buf[last] == '\n';
            }
        }
    }

    @Override
    @SuppressWarnings("resource")
    public void close() throws IOException {
        endAll();
        super.close();
    }

    public void writeModifiers(Set<@NotNull Integer> modifiers) throws IOException {
        for (var entry : Map.<@NotNull Integer, String>of(
                PUBLIC, "public",
                PROTECTED, "protected",
                PRIVATE, "private"
        ).entrySet())
            if (modifiers.contains(entry.getKey())) {
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
            if (modifiers.contains(entry.getKey()))
                writeWhitespaced(entry.getValue());
    }

    public <T> void writeDeclarationList(String key, Collection<T> source, Function<T, String> toString, String separator) throws IOException {
        var iter = source.iterator();
        if (!iter.hasNext())
            return;
        writeWhitespaced(key);
        while (iter.hasNext()) {
            writeWhitespaced(toString.apply(iter.next()));
            if (iter.hasNext()) write(separator);
        }
    }

    public void writeWhitespaced(String str) throws IOException {
        if (!whitespaced && !str.startsWith(" ")) ws();
        write(str);
    }

    public void writeIndented(String str) throws IOException {
        writeIndent(indentLevel);
        write(str);
    }

    public void writeIndent() throws IOException {
        writeIndent(indentLevel);
    }

    public void writeIndent(int c) throws IOException {
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

    private void write(IndentedString string) throws IOException {
        writeIndent(string.indentLevel);
        write(string.string);
    }

    private record IndentedString(String string, int indentLevel) {}
}
