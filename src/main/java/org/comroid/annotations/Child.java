package org.comroid.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * denote a property to be considered a {@link org.comroid.api.tree.Container#child(Class) child} of the containing type
 * {@link Iterable} and {@link java.util.Map} are considered as collections, and their values are provided
 * when used on {@link ElementType#TYPE type} level, all properties are considered
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
public @interface Child {}
