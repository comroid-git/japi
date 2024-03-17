package org.comroid.api.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import org.comroid.api.Polyfill;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.adp.JSON;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Event;
import org.comroid.api.java.Activator;
import org.comroid.api.java.SoftDepend;
import org.comroid.api.java.StackTraceUtils;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.unmodifiableMap;
import static org.comroid.api.Polyfill.uncheckedCast;

@Log
@Value
public class Rabbit {
    private static final Map<URI, Rabbit> $cache = new ConcurrentHashMap<>();
    public static final Map<URI, Rabbit> CACHE = unmodifiableMap($cache);

    public static Wrap<Rabbit> of(@Nullable String uri) {
        if (uri == null) return Wrap.empty();
        final var _uri = Polyfill.uri(uri);
        return SoftDepend.type("com.rabbitmq.client.Connection")
                .map($ -> $cache.computeIfAbsent(_uri, Rabbit::new));
    }

    URI uri;
    Connection connection;
    Map<BindingKeys<?>, Binding<?>> bindings = new ConcurrentHashMap<>();
    Map<String, Binding.Route> routes = new ConcurrentHashMap<>();

    @SneakyThrows
    private Rabbit(URI uri) {
        this.uri = uri;
        var connFactory = new ConnectionFactory();
        connFactory.setUri(uri);
        this.connection = connFactory.newConnection();
    }

    @lombok.Builder(builderMethodName = "binding", buildMethodName = "bind", builderClassName = "Binder")
    public <T extends DataNode> Binding<T> bind(String exchange, String routingKey, Class<? extends T> type) {
        var key = new BindingKeys<>(exchange, routingKey, type);
        return uncheckedCast(bindings.computeIfAbsent(key, Binding::new));
    }

    //@EqualsAndHashCode
    public record BindingKeys<T extends DataNode>(String exchange, String routingKey, Class<T> type) {
        @Override
        public String toString() {
            return "%s -> %s: %s".formatted(exchange, routingKey, StackTraceUtils.lessSimpleName(type));
        }
    }

    @Value
    public class Binding<T extends DataNode> extends Event.Bus<T> {
        String exchange;
        String routingKey;
        Activator<T> ctor;
        @NonFinal
        Route route;

        private Binding(BindingKeys<T> keys) {
            this.exchange = keys.exchange;
            this.routingKey = keys.routingKey;
            this.ctor = Activator.get(keys.type);

            new Timer("Binding Watchdog").schedule(new TimerTask() {
                @Override
                public void run() {
                    touchChannel();
                }
            }, 0, TimeUnit.MINUTES.toMillis(15));
        }

        @SneakyThrows
        public Channel touchChannel() {
            var key = exchange + '@' + routingKey;
            if (route.channel != null) {
                if (route.channel.isOpen())
                    return route.channel;
                try {
                    route.channel.close();
                } catch (Throwable ignored) {
                }
            }
            route = routes.compute(key, (k,v)->createRoute());
            route.channel.basicConsume(route.queue, true, this::handleRabbitData, consumerTag -> {
            });
            return route.channel;
        }

        @SneakyThrows
        public void send(T data) {
            var body = SoftDepend.type("com.fasterxml.jackson.databind.ObjectMapper")
                    .map(ThrowingFunction.logging(log, $$ -> new ObjectMapper().writeValueAsString(data)))
                    .or(data::toSerializedString)
                    .assertion();
            touchChannel().basicPublish(exchange, routingKey, null, body.getBytes());
        }

        private void handleRabbitData(String $, Delivery content) {
            final var body = new String(content.getBody());
            var data = SoftDepend.type("com.fasterxml.jackson.databind.ObjectMapper")
                    .map(ThrowingFunction.logging(log, $$ -> new ObjectMapper().readValue(body, ctor.getTarget())))
                    .or(() -> ctor.createInstance(JSON.Parser.parse(body)))
                    .assertion();
            publish(data);
        }

        @SneakyThrows
        private Route createRoute() {
            var channel = connection.createChannel();
            channel.exchangeDeclare(exchange, "topic");
            var queue = channel.queueDeclare().getQueue();
            channel.queueBind(queue, exchange, routingKey);
            return new Route(channel, queue);
        }

        private record Route(Channel channel, String queue) {}
    }
}
