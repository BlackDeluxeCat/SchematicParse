package sp.struct;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import sp.*;
import sp.utils.*;

import static sp.SchematicParse.floatf;

/** 实体工厂包装类
 * 持有factors信息
 * 解析和处理程序由handler提供 */
public class Entity{
    public float count = 1f;
    public UnlockableContent type;
    public EntityHandler handler;

    /** handler解析得到的factors */
    public Seq<Factor<?>> factors = new Seq<>();
    public IntSeq rates;

    public Table info;

    public Entity(UnlockableContent type){
        this.type = type;
    }

    public void buildHead(Table table){
        table.image(type.uiIcon).size(64f).right();
        table.table(t -> {
            t.defaults().right().growX();
            t.add().grow();
            t.row();
            t.label(() -> Strings.fixed(count, 4)).color(Color.pink).labelAlign(Align.right);
            t.row();
            t.label(() -> ""+Mathf.ceil(count)).fontScale(1.5f).labelAlign(Align.right);
        }).growX();
    }

    public void buildInfos(Table t){
        t.clear();
        t.defaults().minHeight(32f).padTop(4f);
        factors.each(f -> f.enable, f -> {
            f.buildIcon(t, false);
            t.label(() -> FloatStrf.sgnf4.get(f.getRate()));
            t.row();
        });
    }

    public void updateInfos(){
        if(info != null){
            buildInfos(info);
        }
    }

    public void buildConfig(Table table){
        table.field(Strings.fixed(count, 5), floatf, s -> count = Strings.parseFloat(s, 1f)).width(128f).height(64f);
        table.row();
        table.image().height(2f).growX();
        table.row();
        handler.ioHandlers.each(p -> {
            table.add(p.name).growX().row();
            table.table(t -> {
                p.buildConfig(this, t);
            }).growX();
            table.row();
            table.image().height(2f).growX();
            table.row();
        });
    }

    public float getRate(Object type){
        return factors.sumf(f -> type.equals(f.type) ? f.getRate() : 0f);
    }

    public float balance(Object type, float need){
        float rate = getRate(type);
        if(Mathf.zero(rate, 0.00001f) || Mathf.zero(need, 0.00001f)) return 0f;
        return need - (rate * count) / rate;
    }

    public void parse(){
        factors.clear();
        handler.parse(this);
        rates = new IntSeq(factors.size);
    }

    public Entity copy(boolean handlerCopy){
        var copy = new Entity(this.type);
        copy.count = this.count;
        copy.handler = handlerCopy ? this.handler.copy() : this.handler;
        copy.parse();
        return copy;
    }
}