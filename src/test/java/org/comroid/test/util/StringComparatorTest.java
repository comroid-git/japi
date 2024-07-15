package org.comroid.test.util;

import org.comroid.api.func.comp.StringBasedComparator;

import java.util.Arrays;

public class StringComparatorTest {
    public static void main(String[] words) {
        if (words.length == 0)
            words = new String[]{ "hello", "world", "how", "are", "you", "doing", "today" };

        System.out.println("words input  = " + Arrays.toString(words));
        Arrays.sort(words, new StringBasedComparator<>());
        System.out.println("words sorted = " + Arrays.toString(words));
    }
}
