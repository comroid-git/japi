package org.comroid.api.io;

import org.comroid.api.attr.Named;

import java.io.File;

public interface FileGroup extends Named {
    default String getBasePathExtension() {
        return getName() + File.separatorChar;
    }
}
