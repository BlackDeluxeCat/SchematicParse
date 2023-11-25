package sp.struct;

import arc.struct.*;

/** 该类是对同类实体工厂的生产模拟类。
 *  存储一个工厂的IOHandlers
 *  可以用IOHandlersd的name区分特殊IOHandler
 *  计算由simulator实现
 *  存储于Entity，封装后只调用parse，handle，get等工具. */
public class EntityHandler{
    public String name;
    public Entity entity;
    public Seq<IOHandler> ioHandlers = new Seq<>();
    public Seq<Simulator> simulators = new Seq<>();

    public EntityHandler(String n, Entity e, Seq<Simulator> s, IOHandler... hs){
        name = n;
        entity = e;
        simulators.add(s);
        ioHandlers.addAll(hs);
    }

    public EntityHandler(String n, Entity e, Seq<Simulator> s, Seq<IOHandler> hs){
        name = n;
        entity = e;
        simulators.add(s);
        ioHandlers.addAll(hs);
    }

    public EntityHandler addIO(IOHandler io){
        ioHandlers.add(io);
        return this;
    }

    /** 调整倍率等数据 */
    public void handle(){
        simulators.each(s -> s.get(entity));
    }

    public Seq<Factor<?>> seqFactors(){
        return ioHandlers.flatMap(io -> io.factors);
    }

    public EntityHandler copy(Entity e){
        var copy = new EntityHandler(name, e, simulators, ioHandlers.map(IOHandler::copy));
        return copy;
    }

    public interface Simulator{
        void get(Entity e);

        Simulator none = e -> {};

        Simulator overdriveAll = e -> {
            var od = e.factors.find(f -> f.type.equals("overdrive"));
            if(od == null) return;//this shouldn't happen
            float odr = od.enable ? od.rate : 1f;
        };
    }
}

