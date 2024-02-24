package org.comroid.api.net;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.api.Polyfill;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.JSON;
import org.comroid.api.data.seri.Jackson;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Event;
import org.comroid.api.java.Activator;
import org.comroid.api.java.SoftDepend;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableMap;

@Value
public class Rabbit {
    private static final Map<URI, Rabbit> $cache = new ConcurrentHashMap<>();
    public static final Map<URI, Rabbit> CACHE = unmodifiableMap($cache);

    public static Wrap<Rabbit> of(String uri) {
        final var _uri = Polyfill.uri(uri);
        return SoftDepend.type("com.rabbitmq.client.Connection")
                .map($ -> $cache.computeIfAbsent(_uri, Rabbit::new));
    }

    URI uri;
    Connection connection;

    @SneakyThrows
    private Rabbit(URI uri) {
        this.uri = uri;
        var connFactory = new ConnectionFactory();
        connFactory.setUri(uri);
        this.connection = connFactory.newConnection();
    }

    @Value
    public class Binding<T extends DataNode> extends Event.Bus<T> {
        String exchange;
        String routingKey;
        Activator<T> ctor;
        @NonFinal Channel channel;

        public Binding(String exchange, String routingKey, Class<T> type) {
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.ctor = Activator.get(type);

            touchChannel();
        }

        @SneakyThrows
        public Channel touchChannel() {
            if (channel != null) {
                if (channel.isOpen())
                    return channel;
                else channel.close();
            }
            this.channel = connection.createChannel();
            channel.exchangeDeclare(exchange, "topic");
            var queue = channel.queueDeclare().getQueue();
            channel.queueBind(queue, exchange, routingKey);
            channel.basicConsume(queue, true, this::handleRabbitData, consumerTag -> {
            });
            return channel;
        }

        @SneakyThrows
        public void send(T data) {
            var body = data.json().toSerializedString().getBytes();
            touchChannel().basicPublish(exchange, routingKey, null, body);
        }

        private void handleRabbitData(String $, Delivery content) {
            final var body = new String(content.getBody());
            var data = SoftDepend.type("com.fasterxml.jackson.databind.ObjectMapper")
                    .map($$ -> Jackson.JSON.parse(body))
                    .or(() -> JSON.Parser.parse(body))
                    .assertion();
            var conv = ctor.createInstance(data);
            publish(conv);
        }
    }
}
