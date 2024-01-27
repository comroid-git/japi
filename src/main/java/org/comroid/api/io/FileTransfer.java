package org.comroid.api.io;

import lombok.AccessLevel;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.comroid.api.attr.Named;
import org.comroid.api.func.util.DelegateStream;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

@Data
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class FileTransfer {
    URI source;
    URI destination;

    public CompletableFuture<FileTransfer> execute() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var inputMethod = Method.find(source.getScheme());
                var outputMethod = Method.find(destination.getScheme());
                var read = inputMethod.openRead(source);
                var write = outputMethod.openWrite(destination);
                DelegateStream.tryTransfer(read, write);
                return this;
            } catch (Throwable t) {
                throw new RuntimeException("Failed to tranfer " + source + " to " + destination, t);
            }
        });
    }

    public enum Method implements Named {
        http {
            @Override
            public InputStream openRead(URI uri) throws IOException {
                return https.openRead(uri);
            }

            @Override
            public OutputStream openWrite(URI uri) {
                return https.openWrite(uri);
            }
        },
        https {
            @Override
            public InputStream openRead(URI uri) throws IOException {
                return uri.toURL().openStream();
            }

            @Override
            public OutputStream openWrite(URI uri) {
                throw new UnsupportedOperationException("Cannot write to HTTP/S connection");
            }
        },
        file {
            @Override
            public InputStream openRead(URI uri) throws FileNotFoundException {
                return new FileInputStream(uri.getPath());
            }

            @Override
            public OutputStream openWrite(URI uri) throws FileNotFoundException {
                return new FileOutputStream(uri.getPath());
            }
        };


        public abstract InputStream openRead(URI uri) throws IOException;

        public abstract OutputStream openWrite(URI uri) throws IOException;

        public static Method find(@Nullable String scheme) {
            return scheme == null ? https : valueOf(scheme);
        }
    }
}
