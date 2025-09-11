package org.comroid.util;

import org.intellij.lang.annotations.Language;

@SuppressWarnings("ALL")
public class BigotryFilter {
    private BigotryFilter() {
        throw new UnsupportedOperationException();
    }

    public static final @Language("RegExp") String   Separator = "[/,\\s\r\n]+";
    public static final String[] Pronouns = new String[]{ "they", "them" };

    static {
        /*
        try {
            var file = new FileHandle(Path.of(System.getProperty("user.home"), "pronouns.txt").toFile());
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
         */
    }

    public static void init() {}
}
