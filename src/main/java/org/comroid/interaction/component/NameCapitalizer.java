package org.comroid.interaction.component;

import org.comroid.api.text.Capitalization;

public interface NameCapitalizer {
    Capitalization getCapitalization(ComponentType type);

    enum ComponentType {
        /// name of a command group
        GROUP,
        /// name of a callable command
        CALLABLE,
        /// name of a title text
        TITLE,
        /// name of a parameter node
        PARAMETER
    }
}
