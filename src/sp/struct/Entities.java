package sp.struct;

import arc.struct.*;
import mindustry.*;
import mindustry.ctype.*;
import sp.struct.EntityHandler.*;

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
        for(var block : Vars.content.blocks()){
            var entity = get(block);
            entity.handler = new EntityHandler("", entity, Seq.with(Simulator.none, Simulator.overdriveAll), IOHandler.size, IOHandler.consumePower);
        }
    }
}
