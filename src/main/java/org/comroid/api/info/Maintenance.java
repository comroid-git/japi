package org.comroid.api.info;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.comroid.api.attr.Described;
import org.comroid.api.attr.Named;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Maintenance {
    private static final Set<Inspection> inspections = new HashSet<>();
    public static final  Set<Inspection> INSPECTIONS = Collections.unmodifiableSet(inspections);
    private Maintenance() {
        throw new UnsupportedOperationException();
    }

    @Value
    @Builder
    @EqualsAndHashCode(of = { "name" })
    public static class Inspection implements Named, Described {
        String name;
        String format;
        @lombok.Builder.Default @Nullable String description = null;
        Set<CheckResult> checkResults = new HashSet<>();

        {
            inspections.add(this);
        }

        @Value
        @EqualsAndHashCode(of = { "id" })
        public class CheckResult implements Described {
            Inspection inspection = Inspection.this;
            Object   id;
            Object[] formatArgs;

            public CheckResult(Object id, Object... formatArgs) {
                this.id = ID.of(id);
                this.formatArgs = formatArgs;

                checkResults.add(this);
            }

            @Override
            public String getDescription() {
                return format.formatted(formatArgs);
            }
        }
    }
}
