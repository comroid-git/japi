package org.comroid.api.env;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.comroid.api.attr.Named;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.java.SoftDepend;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum MinecraftModEnvironment implements Named {
    Bukkit(SoftDepend.type("org.bukkit.Bukkit")),
    Fabric(SoftDepend.type("net.fabricmc.api.ModInitializer")),
    Forge(SoftDepend.type("net.minecraftforge.common.ForgeConfig"));

    public static final Set<MinecraftModEnvironment> current = detect();
    Wrap<?> dependency;

    private static Set<MinecraftModEnvironment> detect() {
        return Arrays.stream(values())
                .filter(mme -> mme.dependency.isNonNull())
                .collect(Collectors.toUnmodifiableSet());
    }
}
