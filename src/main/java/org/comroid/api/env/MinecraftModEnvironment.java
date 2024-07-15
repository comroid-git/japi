package org.comroid.api.env;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.comroid.api.attr.Named;
import org.comroid.api.func.ext.Wrap;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.comroid.api.java.SoftDepend.type;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum MinecraftModEnvironment implements Named {
    Bukkit(type("org.bukkit.Bukkit")),
    Fabric(type("net.fabricmc.api.ModInitializer")),
    Forge(type("net.minecraftforge.common.ForgeConfig"));

    public static final Set<MinecraftModEnvironment> current = detect();
    Wrap<?> dependency;

    private static Set<MinecraftModEnvironment> detect() {
        return Arrays.stream(values())
                .filter(mme -> mme.dependency.isNonNull())
                .collect(Collectors.toUnmodifiableSet());
    }
}
