package sp.struct;

import arc.func.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import sp.struct.Factor.*;
import sp.utils.*;

import static mindustry.Vars.*;

/** 该类是对实体工厂的同类IO的解析、存储、配置的集合体
 * 存储于Handler中。 */
public class IOHandler{
    public static float ticks = 60f;

    public static Cons3<Entity, IOHandler, Table> cfgBuilder_onlyOneEnabled = (e, h, t) -> {
        var seq = h.getGroup(e);
        t.defaults().uniform().pad(2f);
        for(var f : seq){
            t.button(b -> f.buildRate(b, false), () -> {
                for(var f1 : seq){
                    f1.enable = false;
                }
                f.enable(!f.enable);
            }).style(Styles.flatTogglet).checked(b -> f.enable);
            if(Mathf.mod(seq.indexOf(f),2) == 0) t.row();
        }
    },
    cfgBuilder_justListAll = (e, h, t) -> {
        h.getGroup(e).each(f -> t.table(f::buildEdit).row());
    };



    //原版无需处理ConsumePower的子类
    public static IOHandler size = new IOHandler("size", (e, h) -> {
        if(e.type instanceof Block b){
            h.addCustomF("size", b.size * b.size, true);
        }
    }, cfgBuilder_justListAll);

    public static IOHandler overdrive = new IOHandler("overdrive", (e, h) -> {
        if(e.type instanceof Block b && b.canOverdrive){
            h.addCustomF("overdrive", 1f, true);
        }
    }, cfgBuilder_justListAll);

    public static IOHandler consumePower = new IOHandler("power-in", (e, h) -> {
        if(e.type instanceof Block b && b.hasPower && b.consPower != null){
            h.addCustomF("power", -b.consPower.usage * ticks, !b.consPower.optional);
        }
    }, cfgBuilder_justListAll);

    public static IOHandler oreMine = new IOHandler("drill-ore", (e, h) -> {
        if(e.type instanceof Drill drill){
            int i = 0;
            for(var item : content.items()){
                if(item.equals(drill.blockedItem) || item.hardness > drill.tier) continue;
                h.addContentF(item, ticks / drill.getDrillTime(item) * drill.size * drill.size, i++ == 0);
            }
        }
    }, cfgBuilder_onlyOneEnabled);

    public static IOHandler liquidPump = new IOHandler("pump-liquid", (e, h) -> {
        if(e.type instanceof Pump pump){
            int i = 0;
            for(var item : content.liquids()){
                h.addContentF(item, ticks * pump.pumpAmount * pump.size * pump.size, i++ == 0);
            }
        }
    }, cfgBuilder_onlyOneEnabled);

    public static Func<Attribute, IOHandler> envFloor_Prov = att -> new IOHandler("attribute-floor-" + att.name, (e, h) -> {
        Vars.content.blocks().each(b -> b instanceof Floor, f -> h.addContentF(f, 1, true).continuous(true));
    }, (e, h, t) -> {
        cfgBuilder_justListAll.get(e, h, t);
        t.label(() -> "" + FloatStrf.sgnf4.get(h.getGroup(e).sumf(f -> (f.type instanceof Floor floor ? floor.attributes.get(att) : 0f))));
    });

    public static IOHandler genCrafter = new IOHandler("output", (e, h) -> {
        if(e.type instanceof GenericCrafter gb){
            if(gb.outputItems != null){
                for(var out : gb.outputItems) h.addContentF(out.item, out.amount / gb.craftTime * ticks, true);
            }
            if(gb.outputLiquids != null){
                for(var out : gb.outputLiquids) h.addContentF(out.liquid, out.amount * ticks, true);
            }
        }
    }, cfgBuilder_justListAll);

    public static Func<Float, IOHandler> consumeItem_Prov = craftTime -> new IOHandler("item-in", (e, h) -> {
        if(e.type instanceof Block block){
            for(var cons : block.consumers){
                if(cons instanceof ConsumeItems ci){
                    for(var i : ci.items) h.addContentF(i.item, -i.amount / craftTime * ticks, !cons.booster);
                }
            }
        }
    }, cfgBuilder_justListAll);

    public static IOHandler consumeLiquid = new IOHandler("liquid-in", (e, h) -> {
        if(e.type instanceof Block block){
            for(var cons : block.consumers){
                if(cons instanceof ConsumeLiquid cl) h.addContentF(cl.liquid, -cl.amount * ticks, !cons.booster);
                if(cons instanceof ConsumeLiquids cl){
                    for(var stack : cl.liquids){
                        h.addContentF(stack.liquid, -stack.amount * ticks, !cl.booster);
                    }
                }
            }
        }
    }, cfgBuilder_justListAll);

    public static IOHandler powerGenerator = new IOHandler("power-out", (e, h) -> {
        if(e.type instanceof PowerGenerator pg){
            h.addCustomF("power", pg.powerProduction * ticks, true);
        }
    }, cfgBuilder_justListAll);

    public static Func2<ConsumeItemFilter, Float, IOHandler> consumeItemFilter_Prov = (filter, craftTime) -> new IOHandler("consume-item-filter", (e, h) -> {
        if(e.type instanceof ConsumeGenerator){
            int i = 0;
            for(var item : content.items()){
                if(!filter.filter.get(item)) continue;
                h.addContentF(item, ticks / craftTime, i++ == 0);
            }
        }
    }, cfgBuilder_onlyOneEnabled);

    public static Func2<ConsumeLiquidFilter, Float, IOHandler> consumeLiquidFilter_Prov = (filter, craftTime) -> new IOHandler("consume-item-filter", (e, h) -> {
        if(e.type instanceof ConsumeGenerator){
            int i = 0;
            for(var item : content.liquids()){
                if(!filter.filter.get(item)) continue;
                h.addContentF(item, ticks / craftTime, i++ == 0);
            }
        }
    }, cfgBuilder_onlyOneEnabled);

    public static Func<Float, IOHandler> heatOut_Prov = out -> new IOHandler("heat-out", (e, h) -> {
        h.addCustomF("heat", out, true);
    }, cfgBuilder_justListAll);




    public final String name;
    public Cons2<Entity, IOHandler> parser;
    public Entity currentParsing;
    public Cons3<Entity, IOHandler, Table> configBuilder;    //一个Parser可能解析出多个Factor，例如地板加成，而ui是配置这些Factor的，最适合和parser绑定

    protected IOHandler(String name, Cons2<Entity, IOHandler> parser, Cons3<Entity, IOHandler, Table> configBuilder){
        this.name = name;
        this.parser = parser;
        this.configBuilder = configBuilder;
    }

    /** 调用该方法以自动给Factor分组 */
    public void addFactor(Factor<?> f, Entity e){
        f.group = name;
        e.factors.add(f);
    }

    public CustomFactor addCustomF(String type, float amt, boolean enable){
        var f = new CustomFactor(type, amt, enable);
        addFactor(f, currentParsing);
        return f;
    }

    public UnlockableContentFactor addContentF(UnlockableContent type, float amt, boolean enable){
        var f = new UnlockableContentFactor(type, amt, enable);
        addFactor(f, currentParsing);
        f.group = name;
        return f;
    }

    public Seq<Factor<?>> getGroup(Entity e){
        return getGroup(e, name);
    }

    public Seq<Factor<?>> getGroup(Entity e, String group){
        return e.factors.select(f -> f.group.equals(group));
    }

    /** 生成一些Factor */
    public void parse(Entity e){
        currentParsing = e;
        parser.get(e, this);
        currentParsing = null;
    }

    /** 为IOEntity的配置界面插入相关Factor的配置框。 */
    public void buildConfig(Entity e, Table t){
        configBuilder.get(e, this, t);
    }

    public IOHandler copy(){
        return new IOHandler(name, parser, configBuilder);//副本未初始化
    }
}