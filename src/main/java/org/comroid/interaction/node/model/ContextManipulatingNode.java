package org.comroid.interaction.node.model;

import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.ContextFilter;
import org.comroid.interaction.annotation.Interaction;

import java.util.Arrays;
import java.util.stream.Stream;

public interface ContextManipulatingNode {
    Interaction.Resolved getInteraction();

    default Stream<ContextFilter> getFilter(String key) {
        return Arrays.stream(getInteraction().filter()).filter(it -> it.filter().startsWith(key));
    }

    default Stream<String> getFilterValues(String key) {
        return getFilter(key).map(ContextFilter::value);
    }

    default Stream<ContextDefinition> getDefinition(String key) {
        return Arrays.stream(getInteraction().definitions()).filter(it -> it.value().startsWith(key));
    }

    default Stream<String> getDefinitionValues(String key) {
        return getDefinition(key).flatMap(def -> Arrays.stream(def.expr()));
    }
}
