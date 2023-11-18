package sp.struct;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.world.*;
import sp.struct.Factor.*;

/** 该类是对实体工厂的同类IO的解析、存储、配置的集合体
 * 存储于Handler中。 */
public class IOHandler{
    public static float ticks = 60f;
    public final String name;
    public Seq<Factor<?>> factors = new Seq<>();
    public Cons2<Entity, EntityHandler> parser;
    public Cons2<Entity, Table> configBuilder;    //一个Parser可能解析出多个Factor，例如地板加成，而ui是配置这些Factor的，最适合和parser绑定

    protected IOHandler(String name, Cons2<Entity, EntityHandler> parser, Cons2<Entity, Table> configBuilder){
        this.name = name;
        this.parser = parser;
        this.configBuilder = configBuilder;
    }

    //暂时不清楚Handler是否该传入，那就传入吧！
    /** 生成一些Factor */
    public void parse(Entity e, EntityHandler h){
        factors.clear();
        parser.get(e, h);
    }

    /** 为IOEntity的配置界面插入相关Factor的配置框。 */
    public void buildConfig(Entity e, Table h){
        configBuilder.get(e, h);
    }

    public IOHandler copy(){
        return new IOHandler(name, parser, configBuilder);//副本未初始化
    }

    //原版无需处理ConsumePower的子类
    public static IOHandler consumePower = new IOHandler("power", (e, h) -> {
        if(e.type instanceof Block b && b.hasPower && b.consPower != null){
            e.factors.add(new CustomFactor("power", b.consPower.usage * ticks, !b.consPower.optional));
        }
    }, (e, t) -> {
        e.factors.select(f -> f.type.equals("power")).each(f -> {
            t.table(f::build);
        });
    });

    public static IOHandler consumeLiquid = new IOHandler("liquid", (e, h) -> {
        if(e.type instanceof Block b){
        }
    }, (e, t) -> {

    });
}