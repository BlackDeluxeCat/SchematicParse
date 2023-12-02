package sp.struct;

import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.world.meta.*;
import sp.ui.*;
import sp.utils.*;

import static sp.SchematicParse.*;

/** Production factor type.*/
public class Factor<T>{
    /**Contains instances of every factor. Used for SourceIOEntity.*/
    public static ObjectMap<Object, Factor<?>> factors = new ObjectMap<>();
    public static float smallSize = 32f;

    public final T type;
    public String group;
    public Object tag;

    /** amount per produce*/
    protected float rate;
    /**If false, factor count will be rounded up.*/
    public boolean continuous = true;

    public float efficiency = 1f;
    public boolean enable = true;

    public Factor(T t, float a, boolean en){
        type = t;
        rate = a;
        enable = en;
        factors.put(t, this);
    }

    public Factor<T> rate(float r){
        rate = r;
        return this;
    }

    public Factor<T> enable(boolean en){
        enable = en;
        return this;
    }

    public Factor<T> tag(Object tag){
        this.tag = tag;
        return this;
    }

    public Factor<T> continuous(boolean continuous){
        this.continuous = continuous;
        return this;
    }

    public void efficiency(float eff){
        efficiency = eff;
    }

    public float getRawRate(){
        return rate;
    }

    public float getRate(){
        return enable ? rate * efficiency : 0f;
    }

    /** Must be overwritten. */
    public Factor<T> copy(){
        var n = new Factor<>(type, rate, enable);
        n.group = group;
        n.tag = tag;
        n.continuous = continuous;
        n.efficiency = efficiency;
        return n;
    }

    /** 主要用于物品源工厂自定义界面 */
    public void buildEdit(Table table){
        table.field(Strings.fixed(rate, 3), floatf, s -> rate = Strings.parseFloat(s, 1f)).width(90f).height(smallSize);
    }

    public void buildRate(Table table, boolean n){
        buildIcon(table, n);
        table.label(() -> "" + getRate()).width(90f).height(smallSize);
        table.label(() -> FloatStrf.f2.get(efficiency * 100f) + StatUnit.percent.localized()).fontScale(0.4f);
    }

    public void buildIcon(Table table, boolean name){}

    public static class UnlockableContentFactor extends Factor<UnlockableContent>{
        public UnlockableContentFactor(UnlockableContent unlockableContent, float a, boolean en){
            super(unlockableContent, a, en);
        }

        @Override
        public void buildEdit(Table table){
            table.image(type.uiIcon).size(smallSize).update(i -> i.setColor(enable ? Color.white : Color.gray)).with(c -> {
                c.clicked(() -> enable = !enable);
            });
            super.buildEdit(table);
        }

        @Override
        public void buildIcon(Table table, boolean name){
            table.image(type.uiIcon).size(smallSize);
            if(name) table.add(type.localizedName);
        }

        @Override
        public UnlockableContentFactor copy(){
            var n = new UnlockableContentFactor(type, rate, enable);
            n.group = group;
            n.tag = tag;
            n.continuous = continuous;
            n.efficiency = efficiency;
            return n;
        }
    }

    /** Custom production factor with String name. Suitable for custom power unit.*/
    public static class CustomFactor extends Factor<String>{
        public CustomFactor(String t, float a, boolean en){
            super(t, a, en);
        }

        @Override
        public void buildEdit(Table table){
            table.add(new SPLabel(type, true, true)).size(smallSize).update(i -> i.setColor(enable ? Color.white : Color.gray)).with(c -> {
                c.clicked(() -> enable = !enable);
            });
            super.buildEdit(table);
        }

        @Override
        public void buildIcon(Table table, boolean name){
            table.add(new SPLabel(type, true, true)).size(smallSize).update(i -> i.setColor(enable ? Color.white : Color.gray));
            if(name) table.add(type);
        }

        @Override
        public CustomFactor copy(){
            var n = new CustomFactor(type, rate, enable);
            n.group = group;
            n.tag = tag;
            n.continuous = continuous;
            n.efficiency = efficiency;
            return n;
        }
    }
}