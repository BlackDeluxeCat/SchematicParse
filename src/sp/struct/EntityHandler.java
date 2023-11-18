package sp.struct;

import arc.scene.ui.layout.*;
import arc.struct.*;

/** 该类是对同类实体工厂的生产模拟类。
 *  存储一个工厂的IOHandlers
 *  可以用IOHandlersd的name区分特殊IOHandler
 *  计算由simulator实现
 *  存储于Entity，封装后只调用parse，handle，get，buildConfig等工具
 *  本身不涉及实体工厂的信息，可以赋给多个实体工厂。 */
public class EntityHandler{
    public String name;
    public Entity entity;
    public Seq<IOHandler> ioHandlers = new Seq<>();
    public Simulator simulator;

    public EntityHandler(String n, Entity e, Simulator s, IOHandler... hs){
        name = n;
        entity = e;
        simulator = s;
        ioHandlers.addAll(hs);
    }

    public EntityHandler addIO(IOHandler io){
        ioHandlers.add(io);
        return this;
    }

    public void parse(){
        ioHandlers.each(p -> p.parse(entity, this));
    }

    /** 处理倍率等数据，形成结果 */
    public void handle(){
        simulator.get(this);
    }

    public void getFactors(Seq<Factor<?>> seq){
        ioHandlers.each(p -> p.factors.each(f -> seq.add(f)));
    }

    /** 构建配置界面 */
    public void buildConfig(Entity e, Table table){
        ioHandlers.each(p -> p.buildConfig(e, table));
    }

    public EntityHandler copy(Entity e){
        var copy = new EntityHandler(name, e, simulator, ioHandlers.toArray());
        this.ioHandlers.each(io -> copy.ioHandlers.add(io.copy()));//副本未初始化
        return copy;
    }

    public interface Simulator{
        void get(EntityHandler e);
    }
}

