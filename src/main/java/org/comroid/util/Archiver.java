package org.comroid.util;

import lombok.*;
import org.comroid.api.DelegateStream;
import org.comroid.api.MimeType;
import org.comroid.api.Named;
import org.comroid.api.OnDemand;
import org.comroid.api.info.Log;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Stream;

@Getter
@lombok.extern.java.Log
public enum Archiver implements Named {
    SevenZip("7z", ".7z", false) {
        @NotNull
        @Override
        protected Collection<String> generateCmd(File execPath, String outputPath, String inputDirectory, Stream<String> excludePaths) {
            //7z a -r <output_archive_path> <input_paths> -x!<exclude_pattern_1> -x!<exclude_pattern_2> ...
            //7z a -r backup.7z C:\MyFiles\ -x!*.log -x!*.tmp -x!FolderToExclude\
            final var cmd = new ArrayList<String>();

            cmd.add(execPath.getAbsolutePath());
            cmd.add("a");
            cmd.add("-r");
            cmd.add(outputPath);
            cmd.add(inputDirectory);
            excludePaths.forEach(exclude -> cmd.add("-x!" + exclude));

            return cmd;
        }
    },
    Tar("tar", ".tar.gz", true) {
        private final OnDemand<Variant> variant = new OnDemand<>(execPath.thenApply(Variant::detect));

        @Override
        protected @NotNull Collection<String> generateCmd(File execPath, String outputPath, String inputDirectory, Stream<String> excludePaths) {
            return variant.now().generateCmd(execPath, outputPath, inputDirectory, excludePaths);
        }

        private enum Variant implements Named {
            gnu("tar (GNU tar)") {
                @Override
                protected @NotNull Collection<String> generateCmd(File execPath, String outputPath, String inputDirectory, Stream<String> excludePaths) {
                    //tar -czvf <output_archive_path> --exclude=<exclude_pattern_1> --exclude=<exclude_pattern_2> ... <input_paths>
                    //tar -czvf backup.tar.gz --exclude='*.log' --exclude='*.tmp' --exclude='FolderToExclude/' C:\MyFiles\
                    final var cmd = new ArrayList<String>();

                    cmd.add(execPath.getAbsolutePath());
                    cmd.add("-czvf");
                    cmd.add(outputPath);
                    excludePaths.forEach(exclude -> cmd.add("--exclude='" + exclude + "'"));
                    cmd.add(inputDirectory);

                    return cmd;
                }
            },
            bsdtar {
                @Override
                protected @NotNull Collection<String> generateCmd(File execPath, String outputPath, String inputDirectory, Stream<String> excludePaths) {
                    throw new UnsupportedOperationException("bsdtar is a fucking piece of shit");
                }


                private static Stream<Map.Entry<String, File>> walkDirectory(File directory) {
                    return walkDirectory(directory, directory);
                }
                private static Stream<Map.Entry<String, File>> walkDirectory(File directory, File rootDirectory) {
                    File[] files = directory.listFiles();
                    if (files == null)
                        return Stream.empty();
                    return Stream.concat(
                            Stream.of(files)
                                    .map(file -> new AbstractMap.SimpleImmutableEntry<>(
                                            rootDirectory.toURI().relativize(file.toURI()).getPath(), file)),
                            Stream.of(files)
                                    .filter(File::isDirectory)
                                    .flatMap(subdirectory -> walkDirectory(subdirectory, rootDirectory))
                    );
                }
            };

            private final @Nullable String str;

            Variant() {
                this(null);
            }

            Variant(@Nullable String str) {
                this.str = str;
            }

            @Override
            public String getAlternateName() {
                return Optional.ofNullable(str).orElseGet(this::name);
            }

            protected @NotNull Collection<String> generateCmd(File execPath, String outputPath, String inputDirectory, Stream<String> excludePaths) {
                throw new AbstractMethodError();
            }

            @SneakyThrows
            private static Variant detect(File exec) {
                var execute = Runtime.getRuntime().exec(new String[]{exec.getAbsolutePath(), "--version"});
                try (var execOutput = new BufferedReader(new InputStreamReader(execute.getInputStream()))) {
                    var info = execOutput.readLine();
                    for (var variant : values())
                        if (info.startsWith(variant.getAlternateName()))
                            return variant;
                }
                throw new RuntimeException("Could not detect suitable variant");
            }
        }
    };

    public static final Comparator<Archiver> ReadOnly = Comparator.comparingInt(a -> a.readOnly ? 0 : 1);

    public static Archiver find() {
        return find(Comparator.naturalOrder());
    }

    public static Archiver find(@MagicConstant(flagsFromClass = Archiver.class) Comparator<Archiver> by) {
        return Stream.of(values()).min(by).orElseThrow();
    }

    public final CompletableFuture<File> execPath = new CompletableFuture<>();
    public final String fileExtension;
    public final boolean readOnly;

    Archiver(String exec, String fileExtension, boolean readOnly) {
        this.fileExtension = fileExtension;
        this.readOnly = readOnly;
        // find exec
        var result = PathUtil.findExec(exec);
        if (result.isEmpty())
            Log.at(Level.WARNING, name() + " executable path not found; please set it manually with Archiver." + name() + ".execPath.complete(path)");
        else this.execPath.complete(result.get());
    }

    public boolean available() {
        return execPath.isDone() && !execPath.isCompletedExceptionally();
    }

    @Builder(builderMethodName = "zip", buildMethodName = "execute", builderClassName = "API")
    public CompletableFuture<File> execute(
            Object outputPath, // without extension
            Object inputDirectory,
            @Singular List<Object> excludePatterns
    ) {
        return execPath.thenComposeAsync(exec -> doZip(
                exec, outputPath.toString() + fileExtension,
                String.valueOf(inputDirectory),
                excludePatterns.stream().map(String::valueOf)
        ));
    }

    @SneakyThrows
    protected CompletableFuture<File> doZip(
            final File execPath,
            final String outputPath,
            final String inputDirectory,
            final Stream<String> excludePaths
    ) {
        final var cmd = generateCmd(execPath, outputPath, inputDirectory, excludePaths);
        //var output = String.join(" ", cmd);
        var io = DelegateStream.IO.execute(cmd.toArray(String[]::new));
        //if (Debug.isDebug())
            io.redirectToSystem();
        return io.onClose().thenApply($ -> new File(outputPath));
    }

    @NotNull
    protected Collection<String> generateCmd(File execPath, String outputPath, String inputDirectory, Stream<String> excludePaths) {
        throw new AbstractMethodError();
    }
}
