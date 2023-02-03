package sp.struct;

import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.type.*;

import static sp.SchematicParse.floatf;

public class FactorIO<T>{
    public static float smallSize = 32f;
    public T type;
    /** amount per tick*/
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
        table.add("default");
    }

    public void buildIcon(Table table){
        table.add("default");
    }

    public static class ItemIO extends FactorIO<Item>{
        public ItemIO(Item t, float a, boolean en){
            super(t, a, en);
        }

        @Override
        public void build(Table table){
            table.image(type.uiIcon).size(smallSize).update(i -> i.setColor(enable ? Color.white : Color.gray)).get().clicked(() -> {
                enable = !enable;
            });
            table.field(Strings.autoFixed(rate, 3), floatf, s -> rate = Strings.parseFloat(s, 1f));
        }

        @Override
        public void buildIcon(Table table){
            table.image(type.uiIcon).size(smallSize);
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
            table.image(type.uiIcon).size(smallSize).update(i -> i.setColor(enable ? Color.white : Color.gray)).get().clicked(() -> {
                enable = !enable;
            });
            table.field(Strings.autoFixed(rate, 3), floatf, s -> rate = Strings.parseFloat(s, 1f));
        }

        @Override
        public void buildIcon(Table table){
            table.image(type.uiIcon).size(smallSize);
        }

        @Override
        public LiquidIO copy(){
            return new LiquidIO(type, rate, enable);
        }
    }

    public static class CustomIO extends FactorIO<String>{
        public CustomIO(String t, float a, boolean en){
            super(t, a, en);
        }

        @Override
        public void build(Table table){
            var l = table.add(type).size(smallSize).update(i -> i.setColor(enable ? Color.white : Color.gray)).get();
            l.clicked(() -> {
                enable = !enable;
            });
            l.setFontScaleX(0.5f);
            table.field(Strings.autoFixed(rate, 3), floatf, s -> rate = Strings.parseFloat(s, 1f));
        }

        @Override
        public void buildIcon(Table table){
            table.add(type).size(smallSize).get().setFontScaleX(0.5f);
        }

        @Override
        public CustomIO copy(){
            return new CustomIO(type, rate, enable);
        }
    }
}