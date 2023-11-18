package sp.struct;

import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;

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

    public void build(Table table){
        table.table(this::buildHead);
        table.row();
        table.pane(this::buildBody).growY().minHeight(64f);
    }

    public void buildHead(Table table){
        var image = new Image(type.uiIcon);
        var infos = new Table(t -> {
            t.add().growY();
            t.row();
            t.label(() -> Strings.autoFixed(count, 4)).left();
            t.row();
            t.label(() -> String.valueOf(Mathf.ceil(count))).left();
        });
        table.stack(image, infos).size(64f);
    }

    public void buildBody(Table t){
        t.defaults().minHeight(24f);
        handler.buildConfig(this, t);
        //factors.each(f -> f.enable, f -> t.label(() -> f.formatter.get(f.amount)));
    }

    public void buildConfig(Table table){
        handler.ioHandlers.each(p -> p.buildConfig(this, table));
    }



    public float getRate(Object type){
        return factors.sumf(f -> type.equals(f.type) ? f.getRate() : 0f);
    }

    public float balance(Object type, float need){
        float rate = getRate(type);
        if(Mathf.zero(rate, 0.00001f)) return 0f;
        return need / rate;
    }

    public void parse(){
        factors.clear();
        handler.parse();
        handler.getFactors(factors);
    }

    public Entity copy(){
        var copy = new Entity(this.type);
        copy.count = this.count;
        copy.handler = this.handler.copy(copy);
        copy.parse();
        return copy;
    }
}
