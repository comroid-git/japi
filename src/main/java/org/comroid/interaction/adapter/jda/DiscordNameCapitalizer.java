package org.comroid.interaction.adapter.jda;

import org.comroid.annotations.Instance;
import org.comroid.api.text.Capitalization;
import org.comroid.interaction.component.NameCapitalizer;

enum DiscordNameCapitalizer implements NameCapitalizer {
    @Instance INSTANCE;

    @Override
    public Capitalization getCapitalization(ComponentType type) {
        return type == ComponentType.TITLE ? Capitalization.Title_Case : Capitalization.lower_hyphen_case;
    }
}
