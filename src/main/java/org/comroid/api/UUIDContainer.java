package org.comroid.api;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface UUIDContainer {
    default UUID getUuid() {
        return getUUID();
    }

    @Deprecated
    default UUID getUUID() {
        return getUuid();
    }

    @ToString(of="id")
    @EqualsAndHashCode(of="id")
    class Base implements UUIDContainer {
        protected final UUID id;

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
