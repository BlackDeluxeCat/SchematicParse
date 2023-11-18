package sp.struct;

import arc.struct.*;
import mindustry.ctype.*;

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

    }
}
