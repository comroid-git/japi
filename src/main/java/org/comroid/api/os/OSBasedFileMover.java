package org.comroid.api.os;

import org.comroid.api.io.FileHandle;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public enum OSBasedFileMover implements OSBasedFileProvider {
    WINDOWS(OS.WINDOWS, (file, into) -> {
        final int exitCode = Runtime.getRuntime()
                .exec(String.format("move /Y %s %s", file.getAbsolutePath(), into.getAbsolutePath()))
                .waitFor();

        return into.createSubFile(file.getName());
    }, (file, into) -> {
        final int exitCode = Runtime.getRuntime()
                .exec(String.format("move /Y %s %s", file.getAbsolutePath(), into.getAbsolutePath()))
                .waitFor();

        return into.createSubFile(file.getName());
    }),
    MAC(OS.MAC, (file, into) -> {
        throw new UnsupportedOperationException("Mac File mover unimplemented");
    }, (file, into) -> {
        throw new UnsupportedOperationException("Mac File mover unimplemented");
    }),
    UNIX(OS.UNIX, (file, into) -> {
        final int exitCode = Runtime.getRuntime()
                .exec(String.format("mv %s %s", file.getAbsolutePath(), into.getAbsolutePath()))
                .waitFor();

        return into.createSubFile(file.getName());
    }, (file, into) -> {
        final int exitCode = Runtime.getRuntime()
                .exec(String.format("mv -r %s %s", file.getAbsolutePath(), into.getAbsolutePath()))
                .waitFor();

        return into.createSubFile(file.getName());
    }),
    SOLARIS(OS.SOLARIS, (file, into) -> {
        throw new UnsupportedOperationException("Solaris File mover unimplemented");
    }, (file, into) -> {
        throw new UnsupportedOperationException("Solaris File mover unimplemented");
    });

    public static final OSBasedFileMover current = OSBasedFileProvider.autoDetect(OSBasedFileMover.class).orElse(UNIX);

    private final OS os;
    private final BiFileProcessor fileMover;
    private final BiFileProcessor directoryMover;

    @Override
    public OS getOperatingSystem() {
        return os;
    }

    @Override
    public FileHandle getBaseDirectory() {
        return new FileHandle(".");
    }

    OSBasedFileMover(OS os, BiFileProcessor fileMover, BiFileProcessor directoryMover) {
        this.os = os;
        this.fileMover = fileMover;
        this.directoryMover = directoryMover;
    }

    public CompletableFuture<FileHandle> moveFile(FileHandle from, FileHandle into) {
        return moveFile(from, into, ForkJoinPool.commonPool());
    }

    public CompletableFuture<FileHandle> moveFile(FileHandle from, FileHandle into, Executor executor) {
        into.validateDir();

        return CompletableFuture.supplyAsync(() -> {
            try {
                return fileMover.move(from, into);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Could not move file", e);
            }
        }, executor);
    }

    public CompletableFuture<FileHandle> moveDirectory(FileHandle from, FileHandle into) {
        return moveDirectory(from, into, ForkJoinPool.commonPool());
    }

    public CompletableFuture<FileHandle> moveDirectory(FileHandle from, FileHandle into, Executor executor) {
        into.validateDir();

        return CompletableFuture.supplyAsync(() -> {
            try {
                return directoryMover.move(from, into);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Could not move directory", e);
            }
        }, executor);
    }

    @FunctionalInterface
    interface BiFileProcessor {
        FileHandle move(FileHandle from, FileHandle into) throws IOException, InterruptedException;
    }
}
