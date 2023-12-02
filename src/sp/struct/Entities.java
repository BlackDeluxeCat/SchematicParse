package sp.struct;

import arc.*;
import arc.struct.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import sp.*;

public class Entities{
    public static OrderedMap<UnlockableContent, Entity> defaults = new OrderedMap<>();

    public static Entity get(UnlockableContent content){
        Entity entity = defaults.get(content);
        if(entity == null){
            entity = new Entity(content);
            defaults.put(content, entity);
        }
        return entity;
    }

    public static void generate(){
        Events.fire(SPEvents.PreGenerateEvent.class);

        //TODO 因为比较懒，没有分类编写复用EntityHandler而是逐个new了
        for(var type : Vars.content.blocks()){
            var e = get(type);

            e.handler = new EntityHandler("", Seq.with(Simulator.none, Simulator.overdrive), IOHandler.size, IOHandler.consumePower, IOHandler.overdrive);
            if(e.type instanceof Drill) e.handler.addIO(IOHandler.oreMine);
            if(e.type instanceof Pump) e.handler.addIO(IOHandler.liquidPump);
            if(e.type instanceof GenericCrafter gb){
                e.handler.addIO(IOHandler.genCrafter);
                e.handler.addIO(IOHandler.consumeLiquid);
                e.handler.addIO(IOHandler.consumeItem_Prov.get(gb.craftTime));
            }

            if(e.type instanceof PowerGenerator pg){
                e.handler.addIO(IOHandler.powerGenerator);
                if(pg instanceof ConsumeGenerator cg){
                    if(cg.filterItem != null) e.handler.addIO(IOHandler.consumeItemFilter_Prov.get(cg.filterItem, cg.itemDuration));
                    if(cg.filterLiquid != null) e.handler.addIO(IOHandler.consumeLiquidFilter_Prov.get(cg.filterLiquid, 1f));
                }
                if(pg instanceof ThermalGenerator tg){
                    e.handler.addIO(IOHandler.envFloor_Prov.get(tg.attribute));
                }
                if(pg instanceof HeaterGenerator hg){
                    e.handler.addIO(IOHandler.heatOut_Prov.get(hg.heatOutput));
                }
            }

            Events.fire(SPEvents.GenerateEvent.event.set(type));
        }

        Events.fire(SPEvents.AfterGenerateEvent.class);

        defaults.values().toSeq().each(Entity::parse);
    }
}
