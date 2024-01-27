package org.comroid.api.text;

import lombok.*;
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
@Value
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Capitalization implements Named, Comparator<String> {
    private static final List<Capitalization> $cache = new ArrayList<>();

    @Alias("camelBackCase")
    public static final Capitalization lowerCamelCase = new Capitalization(Case.lower, null); // "lowerCamelCase"
    @Alias("PascalCase")
    public static final Capitalization UpperCamelCase = new Capitalization(Case.Upper, null); // "UpperCamelCase"

    public static final Capitalization lower_snake_case = new Capitalization(Case.lower, '_'); // "lower_snake_case"
    public static final Capitalization Upper_Snake_Case = new Capitalization(Case.Upper, '_'); // "Upper_Snake_Case"
    @Alias("SCREAMING_SNAKE_CASE")
    public static final Capitalization CAPS_SNAKE_CASE = new Capitalization(Case.CAPS, '_');// "CAPS_SNAKE_CASE"

    @Alias("kebab-case")
    public static final Capitalization lower_hyphen_case = new Capitalization(Case.lower, '-'); // "lower-hyphen-case"
    @Alias("Train-Case")
    public static final Capitalization Upper_Hyphen_Case = new Capitalization(Case.Upper, '-'); // "Upper-Hyphen-Case"
    public static final Capitalization CAPS_HYPHEN_CASE = new Capitalization(Case.CAPS, '-');// "CAPS-HYPHEN-CASE"

    public static final Capitalization lower_dot_case = new Capitalization(Case.lower, '.'); // "lower.dot.case"
    public static final Capitalization Upper_Dot_Case = new Capitalization(Case.Upper, '.'); // "Upper.Dot.Case"
    public static final Capitalization CAPS_DOT_CASE = new Capitalization(Case.CAPS, '.');// "CAPS.DOT.CASE"

    public static final Capitalization Title_Case = new Capitalization(Case.Upper, ' '); // "Title Case"

    public static final Context Default = Context.builder().build();
    public static final Context Current = Default;

    {
        $cache.add(this);
    }

    Case capCase;
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
        else score -= $cache.stream()
                .filter(cap -> cap.separator != null)
                .filter(cap -> string.indexOf(cap.separator) != -1)
                .count();

        final var buf = string.toCharArray();
        var prev = capCase.firstChar;
        var find = State.Any;
        for (int i = 0; i < buf.length; i++) {
            var c = buf[i];
            find = State.find(c);
            if (i == 0 && find == capCase.firstChar)
                score++;
            else if (i > 0 && prev == capCase.firstChar && find == capCase.midWord)
                score++;
            else if (i > 1 && prev == capCase.midWord && (find == capCase.midWord || find == capCase.recurringFirstChar
                    || (separator != null && c == separator)))
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
                        word = (count[0]++ == 0 ? to.capCase.firstChar : to.capCase.recurringFirstChar).apply(word , 0);
                        return to.capCase.midWord.apply(word, IntStream.range(1,word.length()).toArray());
                    }).collect(Collectors.joining(to.separator != null ? to.separator.toString() : ""));
        } else {
            // find word beginning with this Case
            final var firstLetters = new ArrayList<@NotNull Integer>();
            final var buf = string.toCharArray();
            var prev = capCase.firstChar;
            for (int i = 0; i < buf.length; i++) {
                var c = buf[i];
                var find = State.find(c);
                if (prev == capCase.midWord && find == capCase.recurringFirstChar)
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
                        ? to.capCase.firstChar
                        : firstLetter
                        ? to.capCase.recurringFirstChar
                        : to.capCase.midWord).applyAsInt(buf[i]));
            }
            return sb.toString();
        }
    }

    public static Wrap<Capitalization> of(final String string) {
        return Wrap.ofOptional($cache.stream()
                // score with each capitalization
                .map(cap -> new AbstractMap.SimpleImmutableEntry<>(cap.score(string), cap))
                .peek(e -> log.finer("%s \t-\t %s \t-\t %s".formatted(string, e.getValue(), e.getKey())))
                // return the one with the highest score
                .max(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue));
    }

    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public enum Case {
        lower(State.Lower, State.Lower, State.Lower),
        Upper(State.Upper, State.Lower, State.Upper),
        CAPS(State.Upper, State.Upper, State.Upper);

        State firstChar;
        State midWord;
        State recurringFirstChar;
    }

    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum State implements IntPredicate, IntUnaryOperator {
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

    @Value
    @Builder
    public static class Context {
        @lombok.Builder.Default Capitalization constants = UpperCamelCase;
        @lombok.Builder.Default Capitalization types = UpperCamelCase;
        @lombok.Builder.Default Capitalization properties = lowerCamelCase;
        @lombok.Builder.Default Capitalization paths = lower_hyphen_case;
        @lombok.Builder.Default Capitalization display = Title_Case;
        @lombok.Builder.Default Capitalization version = lower_dot_case;
    }
}
