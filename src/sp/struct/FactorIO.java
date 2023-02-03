package sp.struct;

import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
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

    public FactorIO(T t, float a, boolean en){
        set(t, a, en);
    }

    public void set(T t, float a, boolean en){
        type = t;
        rate = a;
        enable = en;
    }

    public FactorIO<T> copy(){
        return new FactorIO<>(type, rate, enable);
    }

    /**The copy of each factor can only be configured with table ui.*/
    public void build(Table table){
        table.field(Strings.autoFixed(rate, 3), floatf, s -> rate = Strings.parseFloat(s, 1f)).width(90f);
    }

    public void buildIcon(Table table, boolean name){
    }

    public static class ItemIO extends FactorIO<Item>{
        public ItemIO(Item t, float a, boolean en){
            super(t, a, en);
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
        public ItemIO copy(){
            return new ItemIO(type, rate, enable);
        }
    }

    public static class LiquidIO extends FactorIO<Liquid>{
        public LiquidIO(Liquid t, float a, boolean en){
            super(t, a, en);
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
        public LiquidIO copy(){
            return new LiquidIO(type, rate, enable);
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
            return new CustomIO(type, rate, enable);
        }
    }

    public static class UnitIO extends FactorIO<UnitType>{
        public UnitIO(UnitType type, float a, boolean en){
            super(type, a, en);
        }

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
        public UnitIO copy(){
            return new UnitIO(type, rate, enable);
        }
    }

    public static class BlockIO extends FactorIO<Block>{
        public BlockIO(Block type, float a, boolean en){
            super(type, a, en);
        }

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
        public BlockIO copy(){
            return new BlockIO(type, rate, enable);
        }
    }
}