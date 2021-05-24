package org.comroid.api;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface UUIDContainer {
    UUID getUUID();

    class Base implements UUIDContainer {
        private final UUID id;

        @Override
        public UUID getUUID() {
            return id;
        }

        public Base() {
            this(null);
        }

        public Base(@Nullable UUID id) {
            this.id = id == null ? UUID.randomUUID() : id;
        }
    }

    abstract class Seeded implements UUIDContainer {
        private final UUID id = UUID.nameUUIDFromBytes(idSeed().getBytes());

        @Override
        public UUID getUUID() {
            return id;
        }

        protected abstract String idSeed();
    }
}
