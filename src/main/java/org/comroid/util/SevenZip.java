package org.comroid.util;

import lombok.Builder;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Log
@UtilityClass
public class SevenZip {
    public static final CompletableFuture<File> SevenZipExec;

    static {
        // find exec
        SevenZipExec = new CompletableFuture<>();
        var result = PathUtil.findExec("7z");
        if (result.isEmpty())
            log.warning("7z executable path not found; please set it manually with Seven.ZipExec.complete(path)");
        else SevenZipExec.complete(result.get());
    }

    public static boolean available() {
        return SevenZipExec.isDone() && !SevenZipExec.isCompletedExceptionally();
    }

    @Builder(builderMethodName = "zip", buildMethodName = "execute", builderClassName = "API")
    public static CompletableFuture<File> z(
            Object outputPath, // without extension
            Object inputDirectory,
            @Singular List<Object> excludePatterns
    ) {
        return SevenZipExec.thenComposeAsync(exec -> doZip(
                exec, outputPath.toString() + ".7z",
                String.valueOf(inputDirectory),
                excludePatterns.stream().map(String::valueOf)
        ));
    }

    @SneakyThrows
    private static CompletableFuture<File> doZip(
            final File execPath,
            final String outputPath,
            final String inputDirectory,
            final Stream<String> excludePaths
    ) {
        //7z a -r <output_archive_path> <input_paths> -x!<exclude_pattern_1> -x!<exclude_pattern_2> ...
        //7z a -r backup.7z C:\MyFiles\ -x!*.log -x!*.tmp -x!FolderToExclude\
        final var cmd = new ArrayList<String>();

        cmd.add(execPath.getAbsolutePath());
        cmd.add("a");
        cmd.add("-r");
        cmd.add(outputPath);
        cmd.add(inputDirectory);
        excludePaths.forEach(exclude -> cmd.add("-x!" + exclude));

        //var output = String.join(" ", cmd);

        var exec = Runtime.getRuntime().exec(cmd.toArray(String[]::new));
        return exec.onExit().thenApply($ -> new File(outputPath));
    }
}
