package org.comroid.interaction.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.java.Log;
import org.comroid.annotations.Child;
import org.comroid.api.map.MultiValueMap;
import org.comroid.api.tree.Component;
import org.comroid.eval.MinimalExpression;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.component.response.ResponseConverter;
import org.comroid.interaction.component.response.ResponseHandler;
import org.comroid.interaction.node.MethodNode;
import org.comroid.interaction.node.ParameterNode;
import org.comroid.interaction.node.model.ParentNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Log
@Value
@Child
@Builder
public class InteractionContext extends Component.Base {
    public static InteractionContext.Builder basic(InteractionCore core, String... fullCommand) {
        var context = builder().core(core);

        var parts = Arrays.stream(fullCommand).iterator();
        if (!parts.hasNext()) throw new IllegalStateException("Cannot initialize interaction context; command is empty");
        var part = new String[]{ parts.next() };

        var node = core.getRegistered()
                .stream()
                .flatMap(tree -> tree.getNodes().stream())
                .filter(base -> base.getInteraction().value().equalsIgnoreCase(part[0]))
                .findAny()
                .orElseThrow(noSuchCommand(part[0]));

        while (parts.hasNext()) {
            if (!(node instanceof ParentNode parent)) break;

            part[0] = parts.next();
            var buf = parent.getChildren().stream().filter(it -> it.getInteraction().value().equalsIgnoreCase(part[0])).findAny().orElse(null);
            if (buf == null) break;

            node = buf;
        }

        if (!(node instanceof MethodNode method)) throw new IllegalStateException("Unexpected node: " + node);
        context.node(method);

        var defs = new MultiValueMap<String, Object>();
        for (var def : node.getInteraction().definitions())
            defs.getUnderlying()
                    .computeIfAbsent(def.value(), $ -> new HashSet<>())
                    .addAll(Arrays.stream(def.expression()).map(MinimalExpression::evaluate).filter(Objects::nonNull).toList());

        return context.definitions(defs);
    }

    InteractionCore core;
    MethodNode node;
    @Singular Map<String, Object>        values;
    @Singular Map<ParameterNode, Object> parameters;
    MultiValueMap<String, Object> definitions;

    public Object getValue(String key) {
        return values.getOrDefault(key, null);
    }

    public Object getParameter(ParameterNode node) {
        return parameters.getOrDefault(node, null);
    }

    public void invoke() {
        if (node.getInteraction().async()) {
            component(ResponseHandler.class).ifPresent(it -> it.deferResponse(this));

            CompletableFuture.supplyAsync(() -> node.invoke(this)).thenAccept(this::handleResponse).exceptionally(t -> {
                core.handle(this, t);
                return null;
            });
        } else try {
            handleResponse(node.invoke(this));
        } catch (Throwable t) {
            core.handle(this, t);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleResponse(final Object response) {
        children(ResponseConverter.class).map(converter -> converter.convertResponse(response)).filter(Objects::nonNull).forEach(formatted -> {
            var fType = formatted.getClass();
            children(ResponseHandler.class).filter(handler -> handler.getResponseType().isAssignableFrom(fType))
                    .forEach(handler -> handler.sendResponse(this, formatted));
        });
    }

    private static Supplier<RuntimeException> noSuchCommand(String name) {
        return () -> new IllegalStateException("No such command: " + name);
    }
}
