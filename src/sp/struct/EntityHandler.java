package sp.struct;

import arc.scene.ui.layout.*;
import arc.struct.*;

/** 从实体工厂中抽象出的生产信息聚合类。
 *  是分析工具的存储与组合，为实体工厂处理IO解析、倍率计算、配置界面。
 *  本身不涉及实体工厂的信息，可以赋给多个实体工厂。 */
public class EntityHandler{
    public String name;
    public Seq<Parser> parsers = new Seq<>();
    public Simulator simulator;

    public EntityHandler(){}

    public void addParser(Parser p){
        parsers.add(p);
    }

    public void parse(Entity e){
        parsers.each(p -> p.parse(e, this));
    }

    /** 处理实体的IO倍率 */
    public void handle(Entity e){
        simulator.get(e);
    }

    /** 构建配置界面 */
    public void buildConfig(Entity e, Table table){
        parsers.each(p -> p.buildConfig(e, table));
    }



    public interface Simulator{
        void get(Entity e);
    }
}

