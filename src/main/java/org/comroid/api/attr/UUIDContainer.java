package org.comroid.api.attr;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.comroid.annotations.Ignore;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface UUIDContainer {
    default UUID getUuid() {
        return getUUID();
    }

    @Ignore
    @Deprecated
    default UUID getUUID() {
        return getUuid();
    }

    @ToString(of = "id")
    @EqualsAndHashCode(of = "id")
    class Base implements UUIDContainer {
        protected final UUID id;

        public Base() {
            this(null);
        }

        public Base(@Nullable UUID id) {
            this.id = id == null ? UUID.randomUUID() : id;
        }

        @Override
        public UUID getUUID() {
            return id;
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
