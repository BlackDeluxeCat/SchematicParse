package sp.struct;

import arc.func.*;
import arc.scene.ui.layout.*;
import mindustry.world.*;
import sp.struct.Factor.*;

/** 该类用途是整合与Parser有关的功能，为每个Parser添加名字
 * 使Handler中的Parsers互相感知，并便于扩展 */
public class Parser{
    public static float ticks = 60f;
    public final String name;
    public Cons2<Entity, EntityHandler> parser;
    public Cons2<Entity, Table> configBuilder;    //一个Parser可能解析出多个Factor，例如地板加成，而ui是配置这些Factor的，最适合和parser绑定

    protected Parser(String name, Cons2<Entity, EntityHandler> parser, Cons2<Entity, Table> configBuilder){
        this.name = name;
        this.parser = parser;
    }

    //暂时不清楚Handler是否该传入，那就传入吧！
    /** 为IOEntity生成一些Factor */
    public void parse(Entity e, EntityHandler h){
        parser.get(e, h);
    }

    /** 为IOEntity的配置界面插入相关Factor的配置框。 */
    public void buildConfig(Entity e, Table h){
        configBuilder.get(e, h);
    }

    //原版无需处理ConsumePower的子类
    public static Parser consumePower = new Parser("power", (e, h) -> {
        if(e.type instanceof Block b && b.hasPower && b.consPower != null){
            e.factors.add(new CustomFactor("power", b.consPower.usage * ticks, !b.consPower.optional));
        }
    }, (e, t) -> {
        e.factors.select(f -> f.type.equals("power")).each(f -> {
            t.table(f::build);
        });
    });

    public static Parser consumeLiquid = new Parser("liquid", (e, h) -> {
        if(e.type instanceof Block b){
            b.consumers
        }
    }, (e, t) -> {

    });
}
