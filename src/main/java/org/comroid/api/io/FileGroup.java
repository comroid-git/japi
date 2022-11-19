package org.comroid.api.io;

import org.comroid.api.Named;

import java.io.File;

public interface FileGroup extends Named {
    default String getBasePathExtension() {
        return getName() + File.separatorChar;
    }
}
