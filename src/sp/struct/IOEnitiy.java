package sp.struct;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;

import sp.struct.FactorIO.*;

public class IOEnitiy{
    public UnlockableContent content;
    public Seq<FactorIO<?>> factors = new Seq<>();
    public float count = 1f;

    public IOEnitiy(UnlockableContent type){
        content = type;
    }

    public float getRate(Object type){
        return factors.sumf(t -> t.enable && t.type.equals(type) ? t.rate : 0f);
    }

    public <T> void add(FactorIO<T> factor){
        factors.add(factor);
    }

    public float need(Object type, float need){
        float rate = getRate(type);
        if(Mathf.zero(rate, 0.00001f)) return 0f;
        return need / rate;
    }

    public IOEnitiy copy(){
        var copy = new IOEnitiy(this.content);
        this.factors.each(c -> copy.add(c.copy()));
        return copy;
    }

    public void buildFactors(Table table){
        factors.each(fac -> {
            table.table(fac::build);
            table.row();
        });
    }

    public static class PlanIOEntity extends IOEnitiy{

        public PlanIOEntity(UnlockableContent type){
            super(type);
        }
    }

    public static class SourceIOEntity extends IOEnitiy{
        public static BaseDialog select = new BaseDialog("Source IO Select"){
            {
                addCloseButton();
            }
        };
        public SourceIOEntity(UnlockableContent type){
            super(type);
        }

        @Override
        public void buildFactors(Table table){
            table.button("" + Iconc.add, Styles.cleart, () -> {
                select.cont.pane(p -> {
                    final int[] i = {0};
                    allFactors.each((obj, factor) -> {
                        p.button(b -> factor.buildIcon(b, true), () -> {
                            add(factor.copy());
                            select.hide();
                        }).pad(4f);
                        if(Mathf.mod(++i[0], 12) == 0) p.row();
                    });
                }).grow();
                select.show();
            });

            table.button("" + Iconc.cancel, Styles.cleart, () -> {
                factors.clear();
                buildFactors(table);
            }).size(16f);

            factors.each(fac -> {
                table.table(fac::build);
                table.button("" + Iconc.cancel, Styles.cleart, () -> {
                    factors.remove(fac);
                    buildFactors(table);
                }).size(16f);
                table.row();
            });
        }
    }

    /**Default factors for each unit.*/
    public static Seq<IOEnitiy> defaults = new Seq<>();
    public static Seq<Cons<IOEnitiy>> initruns = new Seq<>();
    /**Contains instances of every factor. Used for SourceIOEntity.*/
    public static ObjectMap<Object, FactorIO<?>> allFactors = new ObjectMap<>();

    public static void init(){
        float timemul = 60f;
        initruns.add(e -> {
            if(e.content instanceof Block block){
                float ticks = Float.MAX_VALUE;
                if(block instanceof GenericCrafter gc) ticks = gc.craftTime;
                if(block instanceof Separator s) ticks = s.craftTime;
                if(block instanceof ConsumeGenerator cg) ticks = cg.itemDuration;
                if(block instanceof ImpactReactor ir) ticks = ir.itemDuration;
                if(block instanceof NuclearReactor nr) ticks = nr.itemDuration;
                if(block instanceof Pump p) ticks = p.consumeTime;
                if(block instanceof Fracker f) ticks = f.itemUseTime;
                if(block instanceof Reconstructor r) ticks = r.constructTime;

                for(var cons : block.consumers){
                    if(cons instanceof ConsumePower cp) e.add(new CustomIO("power", -cp.usage * timemul, !cons.booster));
                    //craft ticks only supports some building consumers
                    if(ticks <= 100000000f && cons instanceof ConsumeItems ci){
                        for(var i : ci.items) e.add(new ItemIO(i.item, -i.amount / ticks * timemul, !cons.booster));
                    }
                    if(cons instanceof ConsumeLiquid cl) e.add(new LiquidIO(cl.liquid, -cl.amount * timemul, !cons.booster));
                }

                if(block instanceof GenericCrafter gb){
                    if(gb.outputItems != null){
                        for(var out : gb.outputItems) e.add(new ItemIO(out.item, out.amount / ticks * timemul, true));
                    }

                    if(gb.outputLiquids != null){
                        for(var out : gb.outputLiquids) e.add(new LiquidIO(out.liquid, out.amount * timemul, true));
                    }
                }else if(block instanceof SolidPump solidPump){
                    e.add(new LiquidIO(solidPump.result, solidPump.pumpAmount * timemul, true));
                }else if(block instanceof Pump pump){
                    for(var liquid : Vars.content.liquids()){
                        e.add(new LiquidIO(liquid, pump.size * pump.size * pump.pumpAmount * timemul, false));
                    }
                }else if(block instanceof Drill drill){
                    for(var b : Vars.content.blocks()){
                        if(b instanceof Floor floor){
                            var item = floor.itemDrop;
                            if(item == null) continue;
                            float normal = item.hardness > drill.tier ? 0f : drill.size * drill.size / (drill.drillTime + item.hardness * drill.hardnessDrillMultiplier), boost = normal * drill.liquidBoostIntensity * drill.liquidBoostIntensity;
                            e.add(new ItemIO(item, normal * timemul, false));
                            e.add(new ItemIO(item, boost * timemul, false));
                        }
                    }
                }else if(block instanceof Separator separator){
                    float sum = 0f;
                    for(var stack : separator.results) sum += stack.amount;
                    for(var stack : separator.results){
                        e.add(new ItemIO(stack.item, stack.amount / sum / ticks * timemul, true));
                    }
                }else if(block instanceof PowerGenerator pg){
                    e.add(new CustomIO("power", pg.powerProduction * timemul, true));
                }else if(block instanceof Reconstructor r){
                    for(UnitType[] uta : r.upgrades){
                        if(uta == null || uta[0] == null || uta[1] == null) continue;
                        e.add(new UnitIO(uta[0], -1f / ticks * timemul, false));
                        e.add(new UnitIO(uta[1], 1f / ticks * timemul, false));
                    }
                }
            }
        });

        reGenerateDefaults();

        Events.on(EventType.ContentInitEvent.class, e -> reGenerateDefaults());
    }

    public static void reGenerateDefaults(){
        Vars.content.blocks().each(block -> {
            var entity = get(block);
            entity.factors.clear();
            initruns.each(cons -> cons.get(entity));
        });

        defaults.replace(io -> io.content == Blocks.itemSource ? new SourceIOEntity(Blocks.itemSource) : io);
    }

    //TODO generate customized IOEntity class.
    public static IOEnitiy get(UnlockableContent content){
        IOEnitiy entity = defaults.find(io -> io.content.equals(content));
        if(entity == null){
            entity = new IOEnitiy(content);
            defaults.add(entity);
        }
        return entity;
    }
}
