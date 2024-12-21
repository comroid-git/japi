package org.comroid.api.io;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;

@Value
@NonFinal
public class WriterDelegate extends Writer {
    Writer delegate;

    @Override
    @MustBeInvokedByOverriders
    public void write(char @NotNull [] buf, int off, int len) throws IOException {
        delegate.write(buf, off, len);
    }

    @Override
    @MustBeInvokedByOverriders
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    @MustBeInvokedByOverriders
    public void close() throws IOException {
        delegate.close();
    }
}
