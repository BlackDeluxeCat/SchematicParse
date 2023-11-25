package sp.utils;

import arc.util.*;

public interface FloatStrf{
    String get(float f);

    FloatStrf fs = String::valueOf;
    FloatStrf f2 = f -> Strings.fixed(f, 2);
    FloatStrf f4 = f -> Strings.fixed(f, 4);
    FloatStrf sgn = f -> f >= 0f ? "+":"";

    FloatStrf sgnf4 = f -> sgn.get(f) + f4.get(f);
}
