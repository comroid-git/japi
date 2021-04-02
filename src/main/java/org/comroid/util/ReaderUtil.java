package org.comroid.util;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReaderUtil {
    private ReaderUtil() {
        throw new UnsupportedOperationException();
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
                int read = 0, index;

                while (read < len && (index = readerIndex.get()) < readers.length) {
                    int nextIndex = readerIndex.incrementAndGet();
                    if (nextIndex >= readers.length && read + 1 >= len)
                        break;

                    boolean isAppend = read != 0;
                    if (isAppend && delimiter != null)
                        buf[read++] = delimiter;
                    int maxRead = len - read;
                    int justRead = readers[index].read(buf, read, maxRead);

                    if (justRead != -1)
                        read += justRead;
                }

                return read;
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (readerIndex) {
                for (Reader reader : readers)
                    reader.close();
            }
        }
    }
}
