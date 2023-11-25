package sp.struct;

import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import sp.utils.*;

import static sp.SchematicParse.floatf;

/** 实体工厂包装类 */
public class Entity{
    public float count = 1f;
    public UnlockableContent type;
    public EntityHandler handler;

    /** 从handler中提取的factor引用 */
    public Seq<Factor<?>> factors = new Seq<>();

    public Entity(UnlockableContent type){
        this.type = type;
    }

    public void buildHead(Table table){
        table.table(t -> {
            t.defaults().left().width(1f);
            t.add().growY();
            t.label(() -> Strings.fixed(count, 4)).color(Color.pink);
            t.row();
            t.label(() -> ""+Mathf.ceil(count)).fontScale(1.5f);
        }).growX();
        table.image(type.uiIcon).size(64f).right();
    }

    public void buildInfos(Table t){
        t.defaults().minHeight(32f).padTop(4f);
        factors.each(f -> f.enable, f -> {
            f.buildIcon(t, false);
            t.label(() -> FloatStrf.sgnf4.get(f.getRate()));
            t.row();
        });
    }

    public void buildConfig(Table table){
        table.field(Strings.fixed(count, 5), floatf, s -> count = Strings.parseFloat(s, 1f)).width(128f).height(64f);
        table.row();
        handler.ioHandlers.each(p -> {
            table.table(t -> {
                p.buildConfig(this, t);
            });
            table.row();
        });
    }

    public float getRate(Object type){
        return factors.sumf(f -> type.equals(f.type) ? f.getRate() : 0f);
    }

    public float balance(Object type, float need){
        float rate = getRate(type);
        if(Mathf.zero(rate, 0.00001f) || Mathf.zero(need, 0.00001f)) return 0f;
        return need / rate;
    }

    public void parse(){
        factors.clear();
        handler.ioHandlers.each(p -> p.parse(handler.entity, handler));
        factors.add(handler.seqFactors());
    }

    public Entity copy(){
        var copy = new Entity(this.type);
        copy.count = this.count;
        copy.handler = this.handler.copy(copy);
        copy.parse();
        return copy;
    }
}
