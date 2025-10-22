package org.comroid.units;

import lombok.Builder;
import lombok.Value;

import static org.jetbrains.annotations.ApiStatus.*;

@Value
@Builder
@Experimental
public class Unit {
    String name;
}
