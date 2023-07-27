package org.comroid.api.io;

import org.comroid.api.ContentParser;
import org.comroid.api.Named;
import org.comroid.api.StringSerializable;
import org.comroid.api.os.OSBasedFileMover;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public final class FileHandle extends File implements Named, ContentParser {
    private final boolean dir;

    @NotNull
    @Override
    public final String getName() {
        return getAbsolutePath();
    }

    @NotNull
    public final String getShortName() {
        return super.getName();
    }

    @Override
    public final String getPrimaryName() {
        return getName();
    }

    @Override
    public final String getAlternateName() {
        return getShortName();
    }

    @Override
    public boolean isDirectory() {
        return dir || super.isDirectory();
    }

    @Override
    public FileHandle getParentFile() {
        return new FileHandle(super.getParentFile().getAbsolutePath(), true);
    }

    public List<String> getLines() {
        final List<String> yields = new ArrayList<>();
        validateExists();

        try (
                FileReader fr = new FileReader(this);
                BufferedReader br = new BufferedReader(fr)
        ) {
            br.lines().forEachOrdered(yields::add);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return yields;
    }

    public boolean isEmpty() {
        String content = getContent();
        return content == null || content.isEmpty();
    }

    public void setContent(StringSerializable content) {
        setContent(content.toSerializedString());
    }

    public void setContent(String content) {
        validateExists();
        try (FileWriter writer = new FileWriter(this, false)) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileHandle(File file) {
        this(file.getAbsolutePath(), file.isDirectory());
    }

    public FileHandle(String absolutePath) {
        this(absolutePath, absolutePath.endsWith(File.separator));
    }

    public FileHandle(File parent, String name, boolean dir) {
        this(parent.getAbsolutePath() + File.separator + name + (dir ? File.separator : ""), dir);
    }

    public FileHandle(String absolutePath, boolean dir) {
        super(absolutePath);

        this.dir = dir;
    }

    public static String guessMimeTypeFromName(String name) {
        String ext = "*";

        if (name.contains("."))
            ext = name.substring(name.lastIndexOf('.'));
        return String.format("*/%s", ext); // todo: improve
    }

    public static FileHandle of(File file) {
        if (file instanceof FileHandle)
            return (FileHandle) file;
        return new FileHandle(file);
    }

    @Override
    public String getContent(boolean createIfAbsent) {
        if (!exists() && createIfAbsent) {
            try {
                createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return String.join("\n", getLines());
    }

    public FileHandle createSubFile(String name) {
        return createSub(name, false);
    }

    public FileHandle createSubDir(String name) {
        if (!isDirectory())
            return getParentFile().createSubDir(name);
        return createSub(name, true);
    }

    public FileHandle createSub(String name, boolean dir) {
        if (!validateDir())
            throw new UnsupportedOperationException("Could not validate directory: " + getAbsolutePath());

        if (dir && !name.endsWith(File.separator))
            name += File.separator;

        FileHandle created = new FileHandle(getAbsolutePath() + File.separator + name, dir);
        if (dir && !created.validateDir())
            throw new UnsupportedOperationException("Could not validate directory: " + created.getAbsolutePath());

        return created;
    }

    public boolean validateExists() {
        try {
            return exists() || (isDirectory() ? mkdirs() : createNewFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean validateDir() {
        final FileHandle parent = getParentFile();
        if (!parent.exists() && !parent.mkdirs())
            throw new UnsupportedOperationException("Could not create parent directory: " + parent.getAbsolutePath());
        if (exists() && !super.isDirectory())
            throw new UnsupportedOperationException("File is not a directory: " + getAbsolutePath());
        if (isDirectory() && !exists() && !mkdirs())
            throw new UnsupportedOperationException("Could not create directory: " + getAbsolutePath());

        return isDirectory();
    }

    @Override
    public boolean mkdirs() {
        if (isDirectory())
            return super.mkdirs();
        else return getParentFile().mkdirs();
    }

    public CompletableFuture<FileHandle> move(FileHandle target) {
        return move(target, ForkJoinPool.commonPool());
    }

    public CompletableFuture<FileHandle> move(FileHandle target, Executor executor) {
        if (isDirectory())
            return OSBasedFileMover.current.moveDirectory(this, target, executor);
        else return OSBasedFileMover.current.moveFile(this, target, executor);
    }

    public boolean containsFile(String fileName) {
        return new File(this, fileName).exists();
    }

    public boolean hasFileContent(String fileName) {
        FileHandle f = createSubFile(fileName);
        return f.exists() && !f.isEmpty();
    }

    public Reader openReader() {
        try {
            return new FileReader(this);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Writer openWriter() {
        return openWriter(false);
    }

    public Writer openWriter(boolean append) {
        try {
            return new FileWriter(this, append);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
