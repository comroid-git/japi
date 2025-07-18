package org.comroid.api.config;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.comroid.annotations.Ignore;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.attr.Aliased;
import org.comroid.api.attr.Named;
import org.comroid.api.config.adapter.TypeAdapter;
import org.comroid.api.data.bind.DataStructure;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.MimeType;
import org.comroid.api.data.seri.type.ArrayValueType;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.exc.ThrowingSupplier;
import org.comroid.api.func.ext.Context;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Debug;
import org.comroid.api.func.util.Pair;
import org.comroid.api.io.FileHandle;
import org.comroid.api.java.Activator;
import org.comroid.api.java.JITAssistant;
import org.comroid.api.text.Capitalization;
import org.comroid.api.tree.UncheckedCloseable;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.comroid.api.Polyfill.*;
import static org.comroid.api.text.Markdown.*;

@Value
public class ConfigurationManager<T extends DataNode> {
    Context          context;
    DataStructure<T> struct;
    File             file;
    MimeType         dataType;
    T                config;
    @NonFinal Instant timestamp = Instant.EPOCH;

    public ConfigurationManager(Context context, Class<T> type, String filePath) {
        this(context, type, filePath, MimeType.JSON);
    }

    public ConfigurationManager(Context context, Class<T> type, String filePath, MimeType dataType) {
        this.context  = context;
        this.struct   = DataStructure.of(type);
        this.file     = new FileHandle(filePath);
        this.dataType = dataType;
        this.config   = ctor();

        invokeAdapters();
    }

    public T initialize() {
        if (!file.exists() && (file.getParentFile().exists() || file.getParentFile()
                .mkdirs())) save(); // save default config
        reload();
        return config;
    }

    public void reload() {
        reload(false);
    }

    @SneakyThrows
    public void reload(boolean force) {
        if (!force && ftime().isBefore(timestamp)) return; // reload is not necessary

        DataNode node;
        try (
                var fis = new FileInputStream(file); var isr = new InputStreamReader(fis);
                var br = new BufferedReader(isr)
        ) {
            var data = br.lines().collect(Collectors.joining());
            node = Objects.requireNonNull(dataType.getDeserializer(), "No deserializer set for " + dataType)
                    .apply(data);
        }
        Objects.requireNonNull(node, "No data");

        setSelfAndChildrenRecursive(struct, config, node);
        this.timestamp = Instant.now();
    }

    private void setSelfAndChildrenRecursive(DataStructure<?> struct, Object it, DataNode data) {
        for (var property : struct.getProperties()) {
            var        node     = data.get(property.getName());
            var        propType = property.getType();
            Object     value    = null;
            Class<?>[] classes  = null;
            if (property.isAnnotationPresent(Adapt.class)) {
                classes = property.getAnnotation(Adapt.class).value();
                JITAssistant.prepare(classes);
                value = Arrays.stream(classes)
                        .flatMap($ -> Stream.ofNullable(TypeAdapter.CACHE.getOrDefault(property.getType()
                                .getTargetClass(), null)))
                        .flatMap(adp -> adp.deserialize(context, adp.parseSerialized(node.asString())).stream())
                        .findAny()
                        .orElse(null);
            }

            if (propType instanceof ArrayValueType<?>) {
                var componentType = propType.getTargetClass().getComponentType();
                var array         = (Object[]) Array.newInstance(componentType, node.size());
                for (int i = 0; i < node.size(); i++) {
                    var each = node.get(i);
                    array[i] = Activator.get(componentType).createInstance(each);
                }
                value = array;
            }

            if (propType.isStandard() || propType.isArray() || classes != null) {
                if (propType.isStandard() && value == null) value = propType.parse(node.asString());
                property.setFor(it, uncheckedCast(value));
            } else //noinspection ConstantValue <- false positive for some reason
                if (node != null) setSelfAndChildrenRecursive(DataStructure.of(propType.getTargetClass()),
                        property.getFrom(it),
                        node);
        }
    }

    @SneakyThrows
    public void save() {
        DataNode data = config;
        if (data == null) return;
        var prefix = dataType.getSerializerPrefix();
        if (prefix != null) data = prefix.apply(data);
        try (var fos = new FileOutputStream(file)) {
            fos.write(data.toSerializedString().getBytes(StandardCharsets.UTF_8));
        }
        timestamp = ftime();
    }

    private void invokeAdapters() {
        invokePropertyAdaptersRecursive(context, config, struct, config);
    }

    private Instant ftime() {
        return Instant.ofEpochMilli(file.lastModified());
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private T ctor() {
        return (T) struct.getType().getConstructor().newInstance();
    }

    private void invokePropertyAdaptersRecursive(Context context, DataNode node, DataStructure<?> struct, Object it) {
        if (node == null || node.isNull()) return;
        for (var property : struct.getProperties()) {
            var propType = property.getType();
            if (propType.isStandard()) continue;
            var propClass  = propType.getTargetClass();
            var propStruct = DataStructure.of(propClass);

            // try recurse
            invokePropertyAdaptersRecursive(context, node.get(property.getName()), propStruct, property.getFrom(it));

            // initialize dependencies
            if (!property.isAnnotationPresent(Adapt.class)) continue;
            JITAssistant.prepare(property.getAnnotation(Adapt.class).value()).join();

            // find & apply adapter
            Optional.ofNullable(TypeAdapter.CACHE.getOrDefault(propClass, null)).map(adp -> {
                var key = property.getName() + Capitalization.Title_Case.convert(adp.getNameSuffix());
                @SuppressWarnings("OptionalOfNullableMisuse") var value = Optional.ofNullable(node.get(key))
                        .map(n -> n.as(adp.getSerialized()))
                        .orElseGet(() -> Annotations.defaultValue(property));
                return adp.deserialize(context, uncheckedCast(value)).get();
            }).ifPresent(value -> property.setFor(it, uncheckedCast(value)));
        }
    }

    public interface Presentation extends UncheckedCloseable {
        void clear();

        void refresh();

        default void resend() {
            clear();
            refresh();
        }
    }

    @Value
    @NonFinal
    public class Presentation$JDA extends ListenerAdapter implements Presentation {
        TextChannel channel;

        public Presentation$JDA(TextChannel channel) {
            this.channel = channel;

            channel.getJDA().addEventListener(this);
        }

        protected boolean checkOutOfContext(GenericInteractionCreateEvent event) {
            return false;
        }

        @Override
        public void clear() {
            List<Message> ls = List.of();
            do {
                if (!ls.isEmpty()) channel.deleteMessages(ls).complete();
                ls = channel.getHistory().retrievePast(100).complete();
            } while (!ls.isEmpty());
        }

        @Override
        public void refresh() {
            for (var property : struct.getProperties())
                if (!property.isAnnotationPresent(Ignore.class)) sendAttributeMessageRecursive(property.getName(),
                        property,
                        config,
                        1);
        }

        private void sendAttributeMessageRecursive(
                String fullName, DataStructure<?>.Property<?> property, Object it, int level) {
            if (it == null) {
                Debug.log(fullName + " is null");
                return;
            }
            Debug.log(fullName + " is not null");

            var title = IntStream.range(0, level)
                                .mapToObj($ -> "#")
                                .collect(Collectors.joining()) + " Config Value " + Code.apply(fullName);
            var desc    = property.getDescription();
            var current = property.getFrom(it);
            var propType  = property.getType();
            var propClass = propType.getTargetClass();
            var text = """
                    %s%s
                    ```
                    %s: %s
                    ```
                    """.formatted(title,
                    desc.isEmpty() ? "" : "\n> " + String.join("\n> ", desc),
                    propType.getTargetClass().getSimpleName(),
                    current);

            ifs:
            {
                if (property.isAnnotationPresent(Adapt.class)) {
                    // prepare dependencies
                    JITAssistant.prepare(property.getAnnotation(Adapt.class).value()).join();

                    // try send mentionable selection box
                    if (IMentionable.class.isAssignableFrom(propClass)) {
                        // choose SelectTarget
                        var target  = getSelectTarget(property);
                        var builder = EntitySelectMenu.create(fullName, target).setRequiredRange(1, 1);

                        // set default value
                        EntitySelectMenu.DefaultValue def;
                        if (current instanceof ISnowflake flake) current = flake.getIdLong();
                        if (current != null) {
                            var currentId = (long) current;
                            if (Channel.class.isAssignableFrom(propClass)) def = EntitySelectMenu.DefaultValue.channel(
                                    currentId);
                            else if (User.class.isAssignableFrom(propClass)) def = EntitySelectMenu.DefaultValue.user(
                                    currentId);
                            else if (Role.class.isAssignableFrom(propClass)) def = EntitySelectMenu.DefaultValue.role(
                                    currentId);
                            else
                                throw new IllegalArgumentException("Invalid mentionable: " + propClass.getCanonicalName());
                            builder.setDefaultValues(def);
                        }

                        // choose ChannelType if necessary
                        if (target == EntitySelectMenu.SelectTarget.CHANNEL) builder.setChannelTypes(Arrays.stream(
                                        ChannelType.values())
                                .filter(type -> type.getInterface().isAssignableFrom(propClass))
                                .filter(type -> type != ChannelType.UNKNOWN)
                                .toList());

                        // send mentionable selection box
                        channel.sendMessage(text).addActionRow(builder.build()).queue();
                    } else break ifs;
                } else if (propClass.isEnum()) {
                    // prepare enum selection box
                    var menu = StringSelectMenu.create(fullName);
                    Arrays.stream(propClass.getFields())
                            .filter(Field::isEnumConstant)
                            .forEach(field -> menu.addOption(Aliased.$(field)
                                    .findAny()
                                    .or(() -> Optional.ofNullable(ThrowingSupplier.sneaky(() -> Named.$(field.get(null)))
                                            .get()))
                                    .orElseGet(field::getName), field.getName(), Annotations.descriptionText(field)));

                    // send enum selection box
                    channel.sendMessage(text).addActionRow(menu.build()).queue();
                } else if (!propType.isStandard()) {
                    var from   = current;
                    var struct = DataStructure.of(propClass);
                    for (var subProperty : struct.getProperties())
                        if (!subProperty.isAnnotationPresent(Ignore.class)) sendAttributeMessageRecursive(fullName + '.' + subProperty.getName(),
                                subProperty,
                                from,
                                level + 1);
                } else break ifs;
                return;
            }

            // just send a simple textbox-based editor message
            channel.sendMessage(text).addActionRow(Button.primary(fullName, "Change Value...")).queue();
        }

        @Override
        public void close() {
            channel.getJDA().removeEventListener(this);
        }

        @Override
        public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
            event.deferReply().submit().thenCompose(hook -> {
                if (checkOutOfContext(event)) return CompletableFuture.completedFuture(null);
                event.replyModal(Modal.create(event.getComponentId(), event.getComponentId())
                        .addActionRow(TextInput.create("newValue", "New Value", TextInputStyle.SHORT)
                                .setPlaceholder(config.get(event.getComponentId().split("\\.")).asString())
                                .build())
                        .build()).queue();
                return hook.sendMessage("Done!").setEphemeral(false).submit();
            }).exceptionally(Debug.exceptionLogger("Internal error when handling interaction"));
        }

        @Override
        public void onModalInteraction(@NotNull ModalInteractionEvent event) {
            event.deferReply().submit().thenCompose(hook -> {
                if (checkOutOfContext(event)) return CompletableFuture.completedFuture(null);
                var path     = event.getModalId().split("\\.");
                var locals   = descend(path);
                var propType = locals.getFirst().getType();

                var newValue = event.getValue("newValue").getAsString();
                locals.getFirst().setFor(locals.getSecond(), uncheckedCast(propType.parse(String.valueOf(newValue))));
                save();
                return updateDisplayValue(event.getMessage(), hook, newValue);
            }).exceptionally(Debug.exceptionLogger("Internal error when handling interaction"));
        }

        @Override
        public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
            event.deferReply().submit().thenCompose(hook -> {
                if (checkOutOfContext(event)) return CompletableFuture.completedFuture(null);
                var path     = event.getComponentId().split("\\.");
                var locals   = descend(path);
                var propType = locals.getFirst().getType();

                return event.getValues()
                        .stream()
                        .flatMap(value -> Arrays.stream(propType.getTargetClass().getFields())
                                .filter(Field::isEnumConstant)
                                .filter(field -> value.equals(Aliased.$(field)
                                        .findAny()
                                        .or(() -> Optional.ofNullable(ThrowingSupplier.sneaky(() -> Named.$(field.get(
                                                null))).get()))
                                        .orElseGet(field::getName))))
                        .findAny()
                        .map(ThrowingFunction.sneaky(field -> field.get(null)))
                        .map(value -> {
                            locals.getFirst().setFor(locals.getSecond(), uncheckedCast(value));
                            save();
                            return updateDisplayValue(event.getMessage(), hook, value);
                        })
                        .orElseGet(() -> failedFuture(new IllegalArgumentException("Invalid values: " + event.getValues())));
            }).exceptionally(Debug.exceptionLogger("Internal error when handling interaction"));
        }

        @Override
        public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent event) {
            event.deferReply().submit().thenCompose(hook -> {
                if (checkOutOfContext(event)) return CompletableFuture.completedFuture(null);
                var path   = event.getComponentId().split("\\.");
                var locals = descend(path);

                return event.getValues().stream().findAny().map(mentionable -> {
                    locals.getFirst().setFor(locals.getSecond(), uncheckedCast(mentionable));
                    save();
                    return updateDisplayValue(event.getMessage(), hook, mentionable.getId());
                }).orElseGet(() -> failedFuture(new IllegalArgumentException("Invalid values: " + event.getValues())));
            }).exceptionally(Debug.exceptionLogger("Internal error when handling interaction"));
        }

        private CompletableFuture<?> updateDisplayValue(Message original, InteractionHook hook, Object value) {
            var raw   = original.getContentRaw();
            var start = raw.indexOf("```");
            return original.editMessage(raw.substring(0, start) + "```\n" + value + "\n```")
                    .flatMap($ -> hook.deleteOriginal())
                    .submit();
        }

        private Pair<DataStructure<?>.Property<?>, Object> descend(String... path) {
            if (path.length == 0) throw new IllegalArgumentException("Empty path");
            Wrap<DataStructure<?>.Property<?>> wrap   = uncheckedCast(struct.getProperty(path[0]));
            Object                             holder = config;
            if (wrap.test(prop -> !prop.getType()
                    .isStandard())) holder = wrap.ifPresentMap(prop -> prop.getFrom(config));
            for (var i = 1; i < path.length; i++) {
                final var fi = i;
                final var fh = holder;
                wrap = wrap.map(it -> it.getType().getTargetClass())
                        .map(DataStructure::of)
                        .flatMap(struct -> struct.getProperty(path[fi]));
                if (wrap.test(prop -> !prop.getType().isStandard() && prop.getType().getTargetClass().isInstance(fh))) {
                    Object o = wrap.ifPresentMap(prop -> prop.getFrom(fh));
                    if (o != null) holder = o;
                }
            }
            final var fh = holder;
            return wrap.ifPresentMapOrElseThrow(prop -> new Pair<>(prop, fh),
                    () -> new NoSuchElementException("No such property: " + String.join(".", path)));
        }

        private static EntitySelectMenu.@NotNull SelectTarget getSelectTarget(DataStructure<?>.Property<?> property) {
            EntitySelectMenu.SelectTarget target;
            if (Channel.class.isAssignableFrom(property.getType()
                    .getTargetClass())) target = EntitySelectMenu.SelectTarget.CHANNEL;
            else if (User.class.isAssignableFrom(property.getType()
                    .getTargetClass())) target = EntitySelectMenu.SelectTarget.USER;
            else if (Role.class.isAssignableFrom(property.getType()
                    .getTargetClass())) target = EntitySelectMenu.SelectTarget.ROLE;
            else throw new IllegalArgumentException("Invalid mentionable: " + property.getType()
                        .getTargetClass()
                        .getCanonicalName());
            return target;
        }
    }
}
