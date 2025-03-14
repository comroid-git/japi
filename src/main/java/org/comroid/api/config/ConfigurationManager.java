package org.comroid.api.config;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.attr.Aliased;
import org.comroid.api.attr.Named;
import org.comroid.api.config.adapter.TypeAdapter;
import org.comroid.api.data.bind.DataStructure;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.MimeType;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.exc.ThrowingSupplier;
import org.comroid.api.func.ext.Context;
import org.comroid.api.io.FileHandle;
import org.comroid.api.java.Activator;
import org.comroid.api.java.JITAssistant;
import org.comroid.api.text.Capitalization;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.comroid.api.Polyfill.*;
import static org.comroid.api.text.Markdown.*;

@Value
public class ConfigurationManager<T extends DataNode> {
    UUID             uuid = UUID.randomUUID();
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
        if (!file.exists()) save(); // save default config
        reload();
        return config;
    }

    public void reload() {
        if (ftime().isBefore(timestamp)) return; // reload is not necessary
        this.timestamp = Instant.now();
    }

    @SneakyThrows
    public void save() {
        DataNode data   = config;
        var      prefix = dataType.getSerializerPrefix();
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

    private T ctor() {
        return uncheckedCast(Activator.get(struct.getType()).createInstance(DataNode.Value.NULL));
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
                var key   = property.getName() + Capitalization.Title_Case.convert(adp.getNameSuffix());
                var value = node.get(key).as(adp.getSerialized()).orElseGet(() -> Annotations.defaultValue(property));
                return adp.deserialize(context, uncheckedCast(value));
            }).ifPresent(value -> property.setFor(it, uncheckedCast(value)));
        }
    }

    public interface Presentation {
        void clear();

        void refresh();

        default void resend() {
            clear();
            refresh();
        }
    }

    @Value
    public class Presentation$JDA implements Presentation {
        TextChannel channel;

        @Override
        public void clear() {
            channel.getHistory().retrievePast(100).flatMap(channel::deleteMessages).queue();
        }

        @Override
        public void refresh() {
            for (var property : struct.getProperties())
                sendAttributeMessageRecursive(property.getName(), property, config, 1);
        }

        private void sendAttributeMessageRecursive(String fullName, DataStructure<?>.Property<?> property, Object it, int level) {
            var title     = IntStream.range(0, level).mapToObj($ -> "#").collect(Collectors.joining()) + " Config Value " + Code.apply(fullName);
            var menuId    = uuid.toString() + ':' + fullName;
            var propType  = property.getType();
            var propClass = propType.getTargetClass();

            ifs:
            {
                if (!propType.isStandard()) {
                    var from   = property.getFrom(it);
                    var struct = DataStructure.of(propClass);
                    for (var subProperty : struct.getProperties())
                        sendAttributeMessageRecursive(fullName + '.' + subProperty.getName(), subProperty, from, level + 1);
                } else if (property.isAnnotationPresent(Adapt.class)) {
                    // prepare dependencies
                    JITAssistant.prepare(property.getAnnotation(Adapt.class).value()).join();

                    // try send mentionable selection box
                    if (IMentionable.class.isAssignableFrom(propClass)) {
                        // choose SelectTarget
                        var target  = getSelectTarget(property);
                        var builder = EntitySelectMenu.create(menuId, target).setRequiredRange(1, 1);

                        // set default value
                        EntitySelectMenu.DefaultValue def;
                        var                           currentId = (long) property.getFrom(it);
                        if (Channel.class.isAssignableFrom(propClass)) def = EntitySelectMenu.DefaultValue.channel(currentId);
                        else if (User.class.isAssignableFrom(propClass)) def = EntitySelectMenu.DefaultValue.user(currentId);
                        else if (Role.class.isAssignableFrom(propClass)) def = EntitySelectMenu.DefaultValue.role(currentId);
                        else throw new IllegalArgumentException("Invalid mentionable: " + propClass.getCanonicalName());
                        builder.setDefaultValues(def);

                        // choose ChannelType if necessary
                        if (target == EntitySelectMenu.SelectTarget.CHANNEL) builder.setChannelTypes(Arrays.stream(ChannelType.values())
                                .filter(type -> type.getInterface().isAssignableFrom(propClass))
                                .toList());

                        // send mentionable selection box
                        var listener = new ListenerAdapter[1];
                        listener[0] = new ListenerAdapter() {
                            @Override
                            public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent event) {
                                if (!menuId.equals(event.getComponentId())) return;
                                event.getValues().stream().findAny().ifPresent(mentionable -> property.setFor(it, uncheckedCast(mentionable)));
                                //channel.getJDA().removeEventListener(listener[0]);
                            }
                        };
                        channel.getJDA().addEventListener(listener[0]);
                        channel.sendMessage(title).addActionRow(builder.build()).queue();
                    } else break ifs;
                } else if (propClass.isEnum()) {
                    // prepare enum selection box
                    var menu = StringSelectMenu.create(menuId);
                    Arrays.stream(propClass.getFields())
                            .filter(Field::isEnumConstant)
                            .forEach(field -> menu.addOption(Aliased.$(field)
                                    .findAny()
                                    .or(() -> Optional.ofNullable(ThrowingSupplier.sneaky(() -> Named.$(field.get(null))).get()))
                                    .orElseGet(field::getName), field.getName(), Annotations.descriptionText(field)));

                    // send enum selection box
                    var listener = new ListenerAdapter[1];
                    listener[0] = new ListenerAdapter() {
                        @Override
                        public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
                            if (!menuId.equals(event.getComponentId())) return;
                            event.getValues()
                                    .stream()
                                    .flatMap(value -> Arrays.stream(propClass.getFields())
                                            .filter(Field::isEnumConstant)
                                            .filter(field -> value.equals(Aliased.$(field)
                                                    .findAny()
                                                    .or(() -> Optional.ofNullable(ThrowingSupplier.sneaky(() -> Named.$(field.get(null))).get()))
                                                    .orElseGet(field::getName))))
                                    .findAny()
                                    .map(ThrowingFunction.sneaky(field -> field.get(null)))
                                    .ifPresent(value -> property.setFor(it, uncheckedCast(value)));
                            //channel.getJDA().removeEventListener(listener[0]);
                        }
                    };
                    channel.getJDA().addEventListener(listener[0]);
                    channel.sendMessage(title).addActionRow(menu.build()).queue();
                }
                return;
            }

            // just send a simple textbox-based editor message
            var listener = new ListenerAdapter[1];
            listener[0] = new ListenerAdapter() {
                @Override
                public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
                    if (!(menuId + ":button").equals(event.getComponentId())) return;
                    event.replyModal(Modal.create(menuId + ":modal", title)
                            .addActionRow(TextInput.create("newValue", "New Value", TextInputStyle.SHORT).build())
                            .build()).queue();
                }

                @Override
                public void onModalInteraction(@NotNull ModalInteractionEvent event) {
                    if (!(menuId + ":modal").equals(event.getModalId())) return;
                    var newValue = event.getValue("newValue");
                    var value    = propType.parse(String.valueOf(newValue));
                    property.setFor(it, uncheckedCast(value));
                    //channel.getJDA().removeEventListener(listener[0]);
                }
            };
            channel.getJDA().addEventListener(listener[0]);
            channel.sendMessageEmbeds(new EmbedBuilder().setTitle(title)
                    .setColor(new Color(86, 98, 246))
                    .setDescription(String.join("\n", property.getDescription()))
                    .addField("Current Value [" + Code.apply(propType.getName()) + "]", CodeBlock.apply(String.valueOf(property.getFrom(it))), false)
                    .build()).addActionRow(Button.primary(menuId + ":button", "Change Value...")).queue();
        }

        private static EntitySelectMenu.@NotNull SelectTarget getSelectTarget(DataStructure<?>.Property<?> property) {
            EntitySelectMenu.SelectTarget target;
            if (Channel.class.isAssignableFrom(property.getType().getTargetClass())) target = EntitySelectMenu.SelectTarget.CHANNEL;
            else if (User.class.isAssignableFrom(property.getType().getTargetClass())) target = EntitySelectMenu.SelectTarget.USER;
            else if (Role.class.isAssignableFrom(property.getType().getTargetClass())) target = EntitySelectMenu.SelectTarget.ROLE;
            else throw new IllegalArgumentException("Invalid mentionable: " + property.getType().getTargetClass().getCanonicalName());
            return target;
        }
    }
}
