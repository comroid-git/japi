package org.comroid.api;

public interface Named extends WrappedFormattable {
    @Override
    default String getDefaultFormattedName() {
        return getName();
    }

    @Override
    default String getAlternateFormattedName() {
        return toString();
    }

    String getName();

    class Base implements Named {
        private final String name;

        @Override
        public final String getName() {
            return name;
        }

        protected Base(String name) {
            this.name = name;
        }
    }
}
