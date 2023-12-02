package sp.struct;

import arc.struct.*;

/** 该类是对同类实体工厂的生产过程模拟类。
 *  因为具体数据存储于Entity，该实例可以一对多使用。但基于已有实例进行修改时必须使用copy
 *  解析和配置界面由IOHandlers完成
 *  模拟计算由Simulators完成
 *  封装后只调用parse，handle，get等工具.
 *
 *  预制的EntityHandler编写在Entities文件中 */
public class EntityHandler{
    public String name;
    public Seq<IOHandler> ioHandlers = new Seq<>();
    public Seq<Simulator> simulators = new Seq<>();

    public EntityHandler(String n, Seq<Simulator> s, IOHandler... hs){
        name = n;
        simulators.add(s);
        ioHandlers.addAll(hs);
    }

    public EntityHandler(String n, Seq<Simulator> s, Seq<IOHandler> hs){
        name = n;
        simulators.add(s);
        ioHandlers.addAll(hs);
    }

    public EntityHandler addIO(IOHandler io){
        ioHandlers.add(io);
        return this;
    }

    public EntityHandler removeIO(IOHandler io){
        ioHandlers.remove(io);
        return this;
    }

    public EntityHandler addSim(Simulator sim){
        simulators.add(sim);
        return this;
    }

    public EntityHandler removeSim(Simulator sim){
        simulators.remove(sim);
        return this;
    }

    public void parse(Entity entity){
        ioHandlers.each(p -> p.parse(entity));
    }

    /** 调整倍率等数据 */
    public void handle(Entity entity){
        simulators.each(s -> s.get(entity));
    }

    public EntityHandler copy(){
        var copy = new EntityHandler(name, simulators, ioHandlers.map(IOHandler::copy));
        return copy;
    }
}

