package sp.ui;

import arc.func.*;
import arc.math.*;
import arc.scene.ui.*;

public class SPLabel extends Label{
    public boolean fontAutoSclX = false, fontAutoSclY = false;
    public SPLabel(Prov<CharSequence> sup, boolean autosclX, boolean autosclY){
        super(sup);
        setFontAutoScale(autosclX, autosclY);
    }

    public SPLabel(CharSequence text, boolean autosclX, boolean autosclY){
        super(text);
        setFontAutoScale(autosclX, autosclY);
    }

    public void setFontAutoScale(boolean x, boolean y){
        fontAutoSclX = x;
        fontAutoSclY = y;
    }

    @Override
    public void act(float delta){
        super.act(delta);
        if(fontAutoSclX || fontAutoSclY){
            //ceil(h/scly/lh) >= sclx*lw/w
            setFontScale(1f);
            setWrap(false);
            layout();
            if(fontAutoSclX && !fontAutoSclY) setFontScaleX(Mathf.clamp(height * width / layout.height / layout.width / fontScaleY, 0.1f, 1f));
            else if(!fontAutoSclX && fontAutoSclY) setFontScaleY(Mathf.clamp(height * width / layout.height / layout.width / fontScaleX, 0.1f, 1f));
            else setFontScale(Mathf.clamp(Mathf.sqrt(height * width / layout.height / layout.width), 0.1f, 1f));
            setWrap(true);
        }
    }
}
