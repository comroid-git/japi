package org.comroid.api.text;

import org.comroid.api.func.WrappedFormattable;
import org.jetbrains.annotations.NotNull;

public interface Word extends CharSequence, WrappedFormattable {
    static Plural.Quantified plural(String singular, String pluralAppendix, Number quantity) {
        return plural(singular, pluralAppendix).new Quantified(quantity);
    }

    static Plural plural(String singular, String pluralAppendix) {
        return Plural.cache.computeIfAbsent(singular, $singular -> new Plural($singular, $singular + pluralAppendix));
    }

    static Plural plural(String singular) {
        return Plural.cache.get(singular);
    }

    @Override
    default String getPrimaryName() {
        return toString();
    }

    @Override
    default String getAlternateName() {
        return getPrimaryName();
    }

    default Word word() {
        return this;
    }

    @Override
    default int length() {
        return toString().length();
    }

    @Override
    default char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    @NotNull
    default CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    Word quantify(Number quantity);
}
