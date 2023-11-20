package sp.utils;

import arc.*;

public class ScreenSense{
    public static boolean horizontal(){
        return Core.graphics.getHeight() + 100 < Core.graphics.getWidth();
    }
}
