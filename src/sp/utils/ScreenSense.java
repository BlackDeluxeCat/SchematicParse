package sp.utils;

import arc.*;
import arc.math.*;

public class ScreenSense{
    public static boolean horizontal(){
        return Core.graphics.getHeight() + 100 < Core.graphics.getWidth();
    }

    public static float height(float scl){
        return Core.graphics.getHeight() * scl;
    }

    public static float width(float scl){
        return Core.graphics.getWidth() * scl;
    }

    public static int columns(float width, float cellWidth){
        return Math.max(Mathf.floor(width / cellWidth), 1);
    }
}
