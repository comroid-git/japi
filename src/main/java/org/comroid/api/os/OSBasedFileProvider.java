package org.comroid.api.os;

import org.comroid.api.io.FileGroup;
import org.comroid.api.io.FileHandle;

import java.util.Optional;

public interface OSBasedFileProvider {
    static <E extends Enum<E> & OSBasedFileProvider> Optional<E> autoDetect(Class<E> fromEnum) {
        for (E enumConstant : fromEnum.getEnumConstants()) {
            if (enumConstant.getOperatingSystem().equals(OS.current))
                return Optional.of(enumConstant);
        }

        return Optional.empty();
    }

    OS getOperatingSystem();

    static Builder builder() {
        return new Builder();
    }

    default FileHandle getFile(FileGroup group, String name) {
        return new FileHandle(getBaseDirectory().getAbsolutePath() + group.getBasePathExtension() + name);
    }

    FileHandle getBaseDirectory();

    final class Builder implements org.comroid.api.func.ext.Builder<OSBasedFileProvider> {
        private String windowsBasePath = null;
        private String macBasePath  = null;
        private String unixBasePath = null;
        private String solarisBasePath = null;

        public String getWindowsBasePath() {
            return windowsBasePath;
        }

        public Builder setWindowsBasePath(String path) {
            this.windowsBasePath = path;
            return this;
        }

        public String getMacBasePath() {
            return macBasePath;
        }

        public Builder setMacBasePath(String path) {
            this.macBasePath = path;
            return this;
        }

        public String getUnixBasePath() {
            return unixBasePath;
        }

        public Builder setUnixBasePath(String path) {
            this.unixBasePath = path;
            return this;
        }

        public String getSolarisBasePath() {
            return solarisBasePath;
        }

        public Builder setSolarisBasePath(String path) {
            this.solarisBasePath = path;
            return this;
        }

        public Builder setPathForOS(OS os, String path) {
            switch (os) {
                case WINDOWS:
                    return setWindowsBasePath(path);
                case MAC:
                    return setMacBasePath(path);
                case UNIX:
                    return setUnixBasePath(path);
                case SOLARIS:
                    return setSolarisBasePath(path);
            }

            throw new AssertionError();
        }

        @Override
        public OSBasedFileProvider build() {
            return new Simple(OS.current, getCurrentOSFileHandle());
        }

        private FileHandle getCurrentOSFileHandle() {
            FileHandle handle = null;

            switch (OS.current) {
                case WINDOWS:
                    handle = new FileHandle(windowsBasePath);
                    break;
                case MAC:
                    handle = new FileHandle(macBasePath);
                    break;
                case UNIX:
                    handle = new FileHandle(unixBasePath);
                    break;
                case SOLARIS:
                    handle = new FileHandle(solarisBasePath);
                    break;
            }

            return handle;
        }
    }

    final class Simple implements OSBasedFileProvider {
        private final OS os;
        private final FileHandle baseDir;

        private Simple(OS os, FileHandle baseDir) {
            this.os      = os;
            this.baseDir = baseDir;
        }

        @Override
        public OS getOperatingSystem() {
            return os;
        }

        @Override
        public FileHandle getBaseDirectory() {
            return baseDir;
        }
    }
}
