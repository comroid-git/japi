package org.comroid.util;

import org.comroid.api.StringSerializable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Formattable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class ReaderUtil {
    private ReaderUtil() {
        throw new UnsupportedOperationException();
    }

    @Internal
    public static void main(String[] args) throws IOException {
        String first = "abc";
        String second = "def";

        runTest(first, second, new char[5]);
        runTest(first, second, new char[4]);
        runTest(first, second, new char[8]);
    }

    @Internal
    private static void runTest(String first, String second, char[] buf) throws IOException {
        Reader reader = combine(';', new StringReader(first), new StringReader(second));
        int read = reader.read(buf);
        String content = new String(buf).substring(0, read);

        System.out.printf("read = %d / %d%n", read, buf.length);
        System.out.printf("content = %s%n", content);
    }

    public static Reader combine(Object... parts) {
        return combine(null, parts);
    }

    public static Reader combine(@Nullable Character delimiter, Object... parts) {
        Reader[] readers = new Reader[parts.length];
        for (int i = 0; i < parts.length; i++) {
            final Object it = parts[i];

            if (it instanceof Reader)
                readers[i] = (Reader) it;
            else if (it instanceof InputStream)
                readers[i] = new InputStreamReader((InputStream) it);
            else if (it instanceof CharSequence)
                readers[i] = new StringReader(((CharSequence) it).toString());
            else if (it instanceof StringSerializable)
                readers[i] = new StringReader(((StringSerializable) it).toSerializedString());
            else if (it instanceof Formattable)
                readers[i] = new StringReader(String.format("%s", it));
            else readers[i] = new StringReader(it.toString());
        }
        return combine(delimiter, readers);
    }

    public static Reader combine(InputStream... streams) {
        return combine(null, streams);
    }

    public static Reader combine(@Nullable Character delimiter, InputStream... streams) {
        Reader[] readers = new Reader[streams.length];
        for (int i = 0; i < streams.length; i++)
            readers[i] = new InputStreamReader(streams[i]);
        return combine(delimiter, readers);
    }

    public static Reader combine(Reader... readers) {
        return combine(null, readers);
    }

    public static Reader combine(@Nullable Character delimiter, Reader... readers) {
        return new CombinedReader(delimiter, readers);
    }

    public static Reader peek(Reader reader, Consumer<char[]> action) {
        return new PeekingReader(reader, action);
    }

    public static int transferTo(Reader reader, Writer writer) throws IOException {
        int r, w = 0;
        char[] buf = new char[1024];
        while ((r = reader.read(buf)) != -1) {
            writer.write(buf, 0, r);
            w += r;
        }
        return w;
    }

    private static class PeekingReader extends Reader {
        private final Reader reader;
        private final Consumer<char[]> action;

        public PeekingReader(Reader reader, Consumer<char[]> action) {
            this.reader = reader;
            this.action = action;
        }

        @Override
        public int read(@NotNull char[] cbuf, int off, int len) throws IOException {
            int read = reader.read(cbuf, off, len);
            action.accept(cbuf);
            return read;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }

    private static class CombinedReader extends Reader {
        private final AtomicInteger readerIndex = new AtomicInteger(0);
        private final Character delimiter;
        private final Reader[] readers;

        private CombinedReader(@Nullable Character delimiter, Reader[] readers) {
            this.delimiter = delimiter;
            this.readers = readers;
        }

        @Override
        public int read(final char[] buf, final int off, final int len) throws IOException {
            synchronized (readerIndex) {
                if (readerIndex.get() >= readers.length)
                    return -1;

                int read = 0, index;
                boolean lastWasUnsatisfied = false;

                while (read < len && (index = readerIndex.get()) < readers.length) {
                    int nextIndex = index + 1;
                    if ((nextIndex >= readers.length || read + 1 >= len) && lastWasUnsatisfied)
                        break;

                    if (read != 0 && delimiter != null && lastWasUnsatisfied)
                        buf[read++] = delimiter;
                    int maxRead = len - read;
                    int justRead = readers[index].read(buf, read, maxRead);

                    if (justRead != -1)
                        read += justRead;
                    if (lastWasUnsatisfied && justRead == -1) {
                        lastWasUnsatisfied = false;
                        readerIndex.set(nextIndex);
                        continue;
                    }
                    lastWasUnsatisfied = (justRead == -1 || justRead < maxRead);
                }

                if (lastWasUnsatisfied)
                    readerIndex.incrementAndGet();
                return read;
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (readerIndex) {
                readerIndex.set(readers.length);
                for (Reader reader : readers)
                    reader.close();
            }
        }
    }
}
