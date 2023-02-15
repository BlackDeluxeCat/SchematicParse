package sp.struct;

import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.*;

import static sp.SchematicParse.floatf;

/** Production factor type.*/
public class FactorIO<T>{
    public static float smallSize = 24f;
    public T type;
    /** amount per sec*/
    public float rate;
    public boolean enable = true;
    /**If false, eneity count will be rounded up.*/
    public boolean continuous = true;

    public FactorIO(T t, float a, boolean en){
        set(t, a, en);
    }

    public void set(T t, float a, boolean en){
        type = t;
        rate = a;
        enable = en;
    }

    /** Must be overwritten. */
    public FactorIO<T> copy(){
        var n = new FactorIO<>(type, rate, enable);
        n.continuous = continuous;
        return n;
    }

    /** The copy of each factor can only be configured with table ui. */
    public void build(Table table){
        table.field(Strings.autoFixed(rate, 3), floatf, s -> rate = Strings.parseFloat(s, 1f)).width(90f);
    }

    public void buildIcon(Table table, boolean name){}

    public static class UnlockableContentIO<T extends UnlockableContent> extends FactorIO<T>{
        public UnlockableContentIO(T unlockableContent, float a, boolean en){
            super(unlockableContent, a, en);
        }

        @Override
        public void build(Table table){
            table.image(type.uiIcon).size(smallSize).update(i -> i.setColor(enable ? Color.white : Color.gray)).with(c -> {
                c.clicked(() -> enable = !enable);
            });
            super.build(table);
        }

        @Override
        public void buildIcon(Table table, boolean name){
            table.image(type.uiIcon).size(smallSize);
            if(name) table.add(type.localizedName);
        }

        @Override
        public UnlockableContentIO<T> copy(){
            var n = new UnlockableContentIO<>(type, rate, enable);
            n.continuous = continuous;
            return n;
        }
    }

    public static class ItemIO extends UnlockableContentIO<Item>{
        public ItemIO(Item t, float a, boolean en){
            super(t, a, en);
        }

        @Override
        public ItemIO copy(){
            var n = new ItemIO(type, rate, enable);
            n.continuous = continuous;
            return n;
        }
    }

    public static class LiquidIO extends UnlockableContentIO<Liquid>{
        public LiquidIO(Liquid t, float a, boolean en){
            super(t, a, en);
        }

        @Override
        public LiquidIO copy(){
            var n = new LiquidIO(type, rate, enable);
            n.continuous = continuous;
            return n;
        }
    }

    public static class UnitIO extends UnlockableContentIO<UnitType>{
        public UnitIO(UnitType type, float a, boolean en){
            super(type, a, en);
        }

        @Override
        public UnitIO copy(){
            var n = new UnitIO(type, rate, enable);
            n.continuous = continuous;
            return n;
        }
    }

    public static class BlockIO extends UnlockableContentIO<Block>{
        public BlockIO(Block type, float a, boolean en){
            super(type, a, en);
        }

        @Override
        public BlockIO copy(){
            var n = new BlockIO(type, rate, enable);
            n.continuous = continuous;
            return n;
        }
    }

    /** Custom production factor with String name. Suitable for custom power unit.*/
    public static class CustomIO extends FactorIO<String>{
        public CustomIO(String t, float a, boolean en){
            super(t, a, en);
        }

        @Override
        public void build(Table table){
            table.labelWrap(type).size(smallSize).update(i -> {
                i.setColor(enable ? Color.white : Color.gray);
                i.setFontScale(1f);
                i.setWrap(false);
                i.layout();
                i.setFontScale(Mathf.clamp(Mathf.sqrt(smallSize / i.getGlyphLayout().width), 0.1f, 1f));
                i.setWrap(true);
            }).with(c -> c.clicked(() -> enable = !enable));
            super.build(table);
        }

        @Override
        public void buildIcon(Table table, boolean name){
            table.add(type).size(smallSize).update(i -> {
                i.setColor(enable ? Color.white : Color.gray);
                i.setFontScale(1f);
                i.setWrap(false);
                i.layout();
                i.setFontScale(Mathf.clamp(Mathf.sqrt(smallSize / i.getGlyphLayout().width), 0.1f, 1f));
                i.setWrap(true);
            });
        }

        @Override
        public CustomIO copy(){
            var n = new CustomIO(type, rate, enable);
            n.continuous = continuous;
            return n;
        }
    }
}