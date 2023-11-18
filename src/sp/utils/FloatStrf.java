package sp.utils;

import arc.util.*;

public interface FloatStrf{
    String get(float f);

    FloatStrf intf = String::valueOf;
    FloatStrf f2f = f -> Strings.fixed(f, 2);
    FloatStrf f4f = f -> Strings.fixed(f, 4);
}
