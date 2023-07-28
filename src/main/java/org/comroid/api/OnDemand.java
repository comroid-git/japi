package org.comroid.api;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class OnDemand<T> extends Container.Base implements Provider<T> {
    CompletableFuture<T> delegate;
    @Getter Supplier<T> supplier;
    @Getter @Setter @NonFinal @Nullable T override;

    public OnDemand(Supplier<T> supplier) {
        this.supplier = supplier;
        this.delegate = new CompletableFuture<>();
    }

    public OnDemand(CompletableFuture<T> delegate) {
        this.delegate = delegate;
        this.supplier = delegate::join;
    }

    @Override
    public CompletableFuture<T> get() {
        if (override != null)
            return CompletableFuture.completedFuture(override);
        if (!delegate.isDone())
            delegate.complete(supplier.get());
        return delegate;
    }

    @Override
    public void closeSelf() {
        delegate.cancel(true);
    }
}
