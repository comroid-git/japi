package org.comroid.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

@ApiStatus.Experimental
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BackgroundTask<T> extends Container.Base implements ValueBox<T>, Runnable, Activatable<BackgroundTask<T>>, Cancellable {
    public static final long NoRepeat = -1;
    @Getter T value;
    @Getter
    Consumer<T> action;
    @Getter long repeatRateMs;
    @Getter OnDemand<Timer> timer = new OnDemand<>(Timer::new);
    @Getter OnDemand<@Nullable Executor> executor = new OnDemand<>(Executors::newSingleThreadExecutor);
    @JsonIgnore FutureTask<T> computation;

    public BackgroundTask(Runnable action) {
        this(null, $->action.run());
    }

    public BackgroundTask(T value, Consumer<T> action) {
        this(value, action, NoRepeat);
    }

    public BackgroundTask(T value, Consumer<T> action, long repeatRateMs) {
        this(value, action, repeatRateMs, null);
    }

    @lombok.Builder
    public BackgroundTask(T value, Consumer<T> action, long repeatRateMs, @Nullable Executor executor) {
        this.value = value;
        this.action = action;
        this.repeatRateMs = repeatRateMs;
        this.executor.setOverride(executor);

        final Runnable run = () -> consume(this.action);
        this.computation = new FutureTask<>(run, null);

        addChildren(value, action, computation);
    }

    @Override
    public @Nullable T get() {
        return value;
    }

    public BackgroundTask<T> activate() {
        return activate(null);
    }

    public BackgroundTask<T> activate(@Nullable Executor executor_) {
        if (repeatRateMs == NoRepeat)
            exec(executor_);
        else timer.block().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                exec(executor_);
            }
        }, 0, repeatRateMs);
        return this;
    }

    public BackgroundTask<T> pause() {
        computation.cancel(false);
        return this;
    }

    private void exec(final @Nullable Executor executor_) {
        var executor = Optional.ofNullable(executor_)
                .orElseGet(this.executor::block);
        assert executor != null;
        executor.execute(this);
    }

    @Override
    public void run() {
        do {
            assert computation != null;
            computation.run();
        } while (repeatRateMs != NoRepeat && !isClosed());
    }

    @Override
    public void cancel(boolean mayInterruptIfRunning) {
        if (computation != null)
            computation.cancel(mayInterruptIfRunning);
    }

    @Override
    public void closeSelf() {
        cancel(true);
    }
}