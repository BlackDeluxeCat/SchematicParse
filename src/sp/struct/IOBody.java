package sp.struct;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;

import sp.struct.FactorIO.*;

public class IOBody{
    public UnlockableContent content;
    public Seq<FactorIO<?>> factors = new Seq<>();
    public float count = 1f;

    public IOBody(UnlockableContent type){
        content = type;
    }

    public float getRate(Object type){
        return factors.sumf(t -> t.enable && t.type.equals(type) ? t.rate : 0f);
    }

    public <T> void add(FactorIO<T> factor){
        factors.add(factor);
    }

    public float need(Object type, float need){
        float rate = factors.sumf(t -> t.type.equals(type) ? t.rate : 0f);
        if(Mathf.zero(rate, 0.00001f)) return 0f;
        return need / rate;
    }

    public IOBody copy(){
        var copy = new IOBody(this.content);
        this.factors.each(c -> copy.add(c.copy()));
        return copy;
    }

    /**Default factors for each unit.*/
    public static Seq<IOBody> defaults = new Seq<>();
    public static Seq<Cons<IOBody>> initruns = new Seq<>();

    public static void init(){
        initruns.add(body -> {
            if(body.content instanceof Block block){
                float ticks = Float.MAX_VALUE;
                if(block instanceof GenericCrafter gc) ticks = gc.craftTime;
                if(block instanceof ConsumeGenerator cg) ticks = cg.itemDuration;
                if(block instanceof ImpactReactor ir) ticks = ir.itemDuration;
                if(block instanceof NuclearReactor nr) ticks = nr.itemDuration;
                if(block instanceof Pump p) ticks = p.consumeTime;
                if(block instanceof Fracker f) ticks = f.itemUseTime;
                if(block instanceof Reconstructor r) ticks = r.constructTime;

                if(ticks <= 100000000f){
                    for(var cons : block.consumers){
                        if(cons instanceof ConsumePower cp) body.add(new CustomIO("power", -cp.usage, !cons.booster));
                        if(cons instanceof ConsumeItems ci){
                            for(var i : ci.items) body.add(new ItemIO(i.item, -i.amount / ticks, !cons.booster));
                        }
                        if(cons instanceof ConsumeLiquid cl) body.add(new LiquidIO(cl.liquid, -cl.amount, !cons.booster));
                    }
                }

                if(block instanceof GenericCrafter gb){
                    if(gb.outputItems != null){
                        for(var out : gb.outputItems) body.add(new ItemIO(out.item, out.amount / ticks, true));
                    }

                    if(gb.outputLiquids != null){
                        for(var out : gb.outputLiquids) body.add(new LiquidIO(out.liquid, out.amount, true));
                    }
                }else if(block instanceof SolidPump solidPump){
                    body.add(new LiquidIO(solidPump.result, solidPump.pumpAmount, true));
                }else if(block instanceof Pump pump){
                    for(var liquid : Vars.content.liquids()){
                        body.add(new LiquidIO(liquid, pump.size * pump.size * pump.pumpAmount, false));
                    }
                }else if(block instanceof Drill drill){
                    for(var b : Vars.content.blocks()){
                        if(b instanceof Floor floor){
                            var item = floor.itemDrop;
                            if(item == null) continue;
                            float normal = item.hardness > drill.tier ? 0f : drill.size * drill.size / (drill.drillTime + item.hardness * drill.hardnessDrillMultiplier), boost = normal * drill.liquidBoostIntensity * drill.liquidBoostIntensity;
                            body.add(new ItemIO(item, normal, false));
                            body.add(new ItemIO(item, boost, false));
                        }
                    }
                }
            }
        });

        reGenerateDefaults();

        Events.on(EventType.ContentInitEvent.class, e -> reGenerateDefaults());
    }

    public static void reGenerateDefaults(){
        Vars.content.blocks().each(block -> {
            var body = get(block);
            body.factors.clear();
            initruns.each(cons -> cons.get(body));
        });
    }

    public static IOBody get(UnlockableContent content){
        IOBody body = defaults.find(io -> io.content.equals(content));
        if(body == null){
            body = new IOBody(content);
            defaults.add(body);
        }
        return body;
    }
}
