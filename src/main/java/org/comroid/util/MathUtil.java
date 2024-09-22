package org.comroid.util;

import org.comroid.api.data.Vector;
import org.jetbrains.annotations.NotNull;

public class MathUtil {
    public static boolean aabb(@NotNull Vector.N3 a, @NotNull Vector.N3 b, @NotNull Vector.N3 position) {
        // Get the minimum and maximum corners of the AABB
        double minX = Math.min(a.getX(), b.getX());
        double maxX = Math.max(a.getX(), b.getX());
        double minY = Math.min(a.getY(), b.getY());
        double maxY = Math.max(a.getY(), b.getY());
        double minZ = Math.min(a.getZ(), b.getZ());
        double maxZ = Math.max(a.getZ(), b.getZ());

        // Check if the position is within the bounds
        return (position.getX() >= minX && position.getX() <= maxX) &&
               (position.getY() >= minY && position.getY() <= maxY) &&
               (position.getZ() >= minZ && position.getZ() <= maxZ);
    }
}
