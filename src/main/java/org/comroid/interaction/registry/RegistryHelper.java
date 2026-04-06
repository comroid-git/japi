package org.comroid.interaction.registry;

import org.comroid.annotations.internal.Annotations;
import org.comroid.api.attr.Named;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.node.GroupNode;
import org.comroid.interaction.node.MethodNode;
import org.comroid.interaction.node.ParameterNode;
import org.comroid.interaction.node.model.InteractionNode;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Internal
public final class RegistryHelper {
    public static Collection<InteractionNode> compileFromClass(@NonNull RegistrySource source, @NonNull Class<?> type) {
        var detached = new ArrayList<InteractionNode>();
        var children = new ArrayList<InteractionNode>();

        for (var subtype : type.getClasses()) {
            var mod = subtype.getModifiers();
            if (!Modifier.isStatic(mod)) continue;

            children.addAll(compileFromClass(source, subtype));
        }

        var base = constructGroup(source, type, children).orElse(null);

        for (var method : type.getMethods()) {
            var mod = method.getModifiers();
            if (source instanceof InstanceRegistry reg && reg.getInstance() != null && Modifier.isStatic(mod)) continue;

            var callable = constructMethod(source, method, base == null ? null : base.toInteractionElement()).orElse(null);
            if (callable == null) continue;

            if (callable.getInteraction().detached()) detached.add(callable);
            else children.add(callable);
        }

        return Stream.concat(detached.stream(), Stream.ofNullable(base)).toList();
    }

    public static Optional<GroupNode> constructGroup(@NonNull RegistrySource source, @NonNull Class<?> type, List<? extends InteractionNode> children) {
        var interaction = type.getAnnotation(Interaction.class);
        if (interaction == null) return Optional.empty();

        return Optional.of(new GroupNode(source, Interaction.Resolved.of(new Interaction.Element(interaction, type), null), type, children));
    }

    public static Optional<MethodNode> constructMethod(@NonNull RegistrySource source, @NonNull Method method, Interaction.@Nullable Element parent) {
        var interaction = method.getAnnotation(Interaction.class);
        if (interaction == null) return Optional.empty();

        return Optional.of(new MethodNode(source,
                Interaction.Resolved.of(new Interaction.Element(interaction, method), parent),
                method,
                Arrays.stream(method.getParameters()).flatMap(p -> constructParameter(source, p).stream()).toList()));
    }

    public static Optional<ParameterNode> constructParameter(@NonNull RegistrySource source, java.lang.reflect.@NonNull Parameter parameter) {
        var annotation = parameter.getAnnotation(Parameter.class);
        if (annotation == null) return Optional.empty();

        return Optional.of(new ParameterNode(source,
                parameter,
                Parameter.Resolved.of(new Parameter.Element(annotation, parameter)),
                Set.of(annotation.completion())));
    }

    public static Optional<String> findName(Interaction.Element element) {
        return findName(element.annotated(), element.interaction(), Interaction::value);
    }

    public static Optional<String> findName(Parameter.Element element) {
        return findName(element.annotated(), element.parameter(), Parameter::value);
    }

    public static Optional<String> findDescription(@NonNull AnnotatedElement element) {
        return Optional.of(element).map(Annotations::descriptionText).filter(Predicate.not(String::isBlank));
    }

    private RegistryHelper() {
        throw new UnsupportedOperationException();
    }

    private static <T> Optional<String> findName(@NonNull AnnotatedElement element, @Nullable T target, @NonNull Function<@NonNull T, String> stringOverride) {
        return Optional.ofNullable(target)
                .map(stringOverride)
                .filter(Predicate.not(Annotations.EMPTY_ATTRIBUTE::equals))
                .or(() -> Annotations.aliases(element).stream().findAny())
                .or(() -> Optional.ofNullable(element).flatMap(it -> Optional.ofNullable(switch (it) {
                    case Class<?> type -> type.getSimpleName();
                    case Member member -> member.getName();
                    case java.lang.reflect.Parameter parameter -> parameter.getName();
                    case Named named -> named.getName();
                    default -> null;
                })));
    }
}
