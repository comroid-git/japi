package org.comroid.api.text;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import lombok.extern.java.Log;
import org.comroid.annotations.Alias;
import org.comroid.api.attr.Named;
import org.comroid.api.func.ext.Wrap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Log
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum Capitalization implements Named, Comparator<String> {
    @Alias("camelBackCase")
    lowerCamelCase(State.Lower, State.Lower, State.Upper, null), // "lowerCamelCase"
    @Alias("PascalCase")
    UpperCamelCase(State.Upper, State.Lower, State.Upper, null), // "UpperCamelCase"

    lower_snake_case(State.Lower, State.Lower, State.Lower, '_'), // "lower_snake_case"
    Upper_Snake_Case(State.Upper, State.Lower, State.Upper, '_'), // "Upper_Snake_Case"
    @Alias("SCREAMING_SNAKE_CASE")
    CAPS_SNAKE_CASE(State.Upper, State.Upper, State.Upper, '_'), // "CAPS_SNAKE_CASE"

    @Alias("kebab-case")
    lower_hyphen_case(State.Lower, State.Lower, State.Lower, '-'), // "lower-hyphen-case"
    @Alias("Train-Case")
    Upper_Hyphen_Case(State.Upper, State.Lower, State.Upper, '-'), // "Upper-Hyphen-Case"
    CAPS_HYPHEN_CASE(State.Upper, State.Upper, State.Upper, '-'), // "CAPS-HYPHEN-CASE"

    lower_dot_case(State.Lower, State.Lower, State.Lower, '.'), // "lower.dot.case"
    Upper_Dot_Case(State.Upper, State.Lower, State.Upper, '.'), // "Upper.Dot.Case"
    CAPS_DOT_CASE(State.Upper, State.Upper, State.Upper, '.'), // "CAPS.DOT.CASE"

    Title_Case(State.Upper, State.Lower, State.Upper, ' '); // "Title Case"

    State firstChar;
    State midWord;
    State recurringFirstChar;
    @Nullable Character separator;

    public static boolean equals(String l, String r) {
        return of(l).map(cap -> cap.convert(r)).test(l::equals);
    }

    @Override
    public int compare(String l, String r) {
        return score(l) - score(r);
    }

    public int score(String string) {
        var score = 0L;

        if (separator != null)
            score += string.chars().filter(separator::equals).count();
        else score -= Arrays.stream(values())
                .filter(cap -> cap.separator != null)
                .filter(cap -> string.indexOf(cap.separator) != -1)
                .count();

        final var buf = string.toCharArray();
        var prev = firstChar;
        var find = State.Any;
        for (int i = 0; i < buf.length; i++) {
            var c = buf[i];
            find = State.find(c);
            if (i == 0 && find == firstChar)
                score++;
            else if (i > 0 && prev == firstChar && find == midWord)
                score++;
            else if (i > 1 && prev == midWord && (find == midWord || find == recurringFirstChar || (separator != null && c == separator)))
                score++;
            else score--;
            prev = find;
        }

        return (int) score;
    }

    public String convert(String string) {
        var current = of(string).assertion("Could not determine capitalization case from string '"+string+'\'');
        if (current == this) return string;
        return current.convert(this, string);
    }

    public String convert(Capitalization to, String string) {
        if (separator != null) {
            final int[] count = new int[]{0};
            return Arrays.stream(string.split(separator.toString()))
                    .map(word -> {
                        word = (count[0]++ == 0 ? to.firstChar : to.recurringFirstChar).apply(word , 0);
                        return to.midWord.apply(word, IntStream.range(1,word.length()).toArray());
                    }).collect(Collectors.joining(to.separator != null ? to.separator.toString() : ""));
        } else {
            // find word beginning with this Case
            final var firstLetters = new ArrayList<@NotNull Integer>();
            final var buf = string.toCharArray();
            var prev = firstChar;
            for (int i = 0; i < buf.length; i++) {
                var c = buf[i];
                var find = State.find(c);
                if (prev == midWord && find == recurringFirstChar)
                    firstLetters.add(i);
                prev = find;
            }

            // convert string
            final var sb = new StringBuilder();
            for (int i = 0; i < buf.length; i++) {
                var firstLetter = firstLetters.contains(i);
                if (firstLetter && to.separator != null)
                    sb.append(to.separator);
                sb.append((char) (i == 0
                        ? to.firstChar
                        : firstLetter
                        ? to.recurringFirstChar
                        : to.midWord).applyAsInt(buf[i]));
            }
            return sb.toString();
        }
    }

    public static Wrap<Capitalization> of(final String string) {
        return Wrap.ofOptional(Arrays.stream(values())
                // score with each capitalization
                .map(cap -> new AbstractMap.SimpleImmutableEntry<>(cap.score(string), cap))
                .peek(e -> log.finer("%s \t-\t %s \t-\t %s".formatted(string, e.getValue(), e.getKey())))
                // return the one with the highest score
                .max(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue));
    }

    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private enum State implements IntPredicate, IntUnaryOperator {
        Any($ -> true, Character::toLowerCase) {
            @Override
            public State invert() {
                return Any;
            }
        },
        Upper(Character::isUpperCase, Character::toUpperCase) {
            @Override
            public State invert() {
                return Lower;
            }
        },
        Lower(Character::isLowerCase, Character::toLowerCase) {
            @Override
            public State invert() {
                return Upper;
            }
        };

        @Delegate
        IntPredicate check;
        @Delegate
        IntUnaryOperator modify;

        public abstract State invert();

        public String apply(String string, int... indices) {
            final var buf = string.toCharArray();
            for (int i = 0; i < buf.length; i++)
                if (Arrays.binarySearch(indices, i) != -1)
                    buf[i] = (char) applyAsInt(buf[i]);
            return new String(buf);
        }

        static State find(int c) {
            return Wrap.ofOptional(Stream.of(Lower, Upper)
                            .filter(it -> it.test(c))
                            .findAny())
                    .orElse(Any);
        }
    }
}
