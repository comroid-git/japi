package org.comroid.interaction.node;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.comroid.api.java.Activator;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.node.model.ContextManipulatingNode;
import org.comroid.interaction.node.model.InteractionNode;
import org.comroid.interaction.node.model.ParameterizedNode;
import org.comroid.interaction.registry.InstanceRegistry;
import org.comroid.interaction.registry.RegistrySource;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public class MethodNode extends InteractionNode implements ParameterizedNode, ContextManipulatingNode {
    Method              method;
    List<ParameterNode> parameters;

    public MethodNode(@NonNull RegistrySource source, Interaction.Resolved interaction, Method method, List<ParameterNode> parameters) {
        super(source, interaction);

        this.method     = method;
        this.parameters = parameters;
    }

    public Optional<ParameterNode> getParameter(String name) {
        return parameters.stream().filter(it -> it.getParameter().value().equals(name)).findAny();
    }

    @Override
    @SneakyThrows
    public Object invoke(InteractionContext context) {
        var params = method.getParameters();
        var args   = new Object[params.length];

        for (var i = 0; i < params.length; i++) {
            var p     = params[i];
            var pType = p.getType();
            var param = parameters.stream().filter(it -> it.getReflect().equals(p)).findAny().orElse(null);

            if (param != null) {
                var    parser = param.getParameter().parser();
                Object value  = context.getParameter(param);

                if (parser != null) value = Activator.get(parser).createInstance().parse(String.valueOf(value));

                args[i] = value;
            } else args[i] = context.component(pType).get();
        }

        var target = source instanceof InstanceRegistry instance ? instance.getInstance() : null;
        if (!Modifier.isStatic(method.getModifiers())) Objects.requireNonNull(target, "target cannot be null on non-static method");

        return method.invoke(target, args);
    }
}
