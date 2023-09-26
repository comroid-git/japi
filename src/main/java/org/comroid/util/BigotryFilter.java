package org.comroid.util;

import lombok.experimental.UtilityClass;
import org.comroid.api.io.FileHandle;
import org.intellij.lang.annotations.Language;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

@UtilityClass
@SuppressWarnings("ALL")
public class BigotryFilter {
    public static final @Language("RegExp") String Separator = "[/,\\s]";
    public static final String[] Pronouns;
    private boolean init = false;

    static {
        try {
            var file = new FileHandle(Path.of(System.getProperty("user.home"), "comroid", "pronouns.txt").toFile());
            file.mkdirs();
            if (file.exists())
                Pronouns = file.getContent(true).split(Separator);
            else {
                System.out.print("Please enter your preferred pronouns separated by '/', ',' or spaces: ");
                Pronouns = new BufferedReader(new InputStreamReader(System.in)).readLine().split(Separator);
                file.setContent(String.join("/", Pronouns));
            }
        } catch (Throwable t) {
            throw new RuntimeException("Could not obtain user pronouns", t);
        }
    }

    public static void init() {}
}
