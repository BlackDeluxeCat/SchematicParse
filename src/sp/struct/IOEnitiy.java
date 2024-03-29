package sp.struct;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import sp.struct.FactorIO.*;

public class IOEnitiy{
    public UnlockableContent content;
    public Seq<FactorIO<?>> factors = new Seq<>();
    public Seq<FactorBucket> buckets = new Seq<>();
    public float count = 1f;

    public IOEnitiy(UnlockableContent type){
        content = type;
    }

    public float getSingleRate(Object type){
        return factors.sumf(t -> t.enable && t.type.equals(type) ? t.rate : 0f) + buckets.sumf(bucket -> bucket.enable ? bucket.factors.sumf(t -> t.enable && t.type.equals(type) ? t.rate : 0f) : 0f);
    }

    public float getRate(Object type){
        return factors.sumf(t -> t.enable && t.type.equals(type) ? t.rate * (t.continuous ? count : Mathf.ceil(count)) : 0f) + buckets.sumf(bucket -> bucket.enable ? bucket.factors.sumf(t -> t.enable && t.type.equals(type) ? t.rate * (t.continuous ? count : Mathf.ceil(count)) : 0f) : 0f);
    }

    public <T> void add(FactorIO<T> factor){
        factors.add(factor);
    }

    public void add(FactorBucket bucket){
        buckets.add(bucket);
    }

    public float need(Object type, float need){
        float rate = getSingleRate(type);
        if(Mathf.zero(rate, 0.00001f)) return 0f;
        return need / rate;
    }

    public IOEnitiy copy(){
        var copy = new IOEnitiy(this.content);
        copy.count = this.count;
        this.factors.each(c -> copy.add(c.copy()));
        this.buckets.each(bucket -> {
            var copyb = new FactorBucket(bucket.name);
            copyb.enable = bucket.enable;
            bucket.factors.each(bf -> copyb.factors.add(bf.copy()));
            copy.add(copyb);
        });
        return copy;
    }

    public void buildFactors(Table table){
        factors.each(fac -> {
            table.table(fac::build);
            table.row();
        });
        buckets.each(bucket -> {
            table.table(bucket::build);
            table.row();
        });
    }

    public static class FactorBucket{
        public String name;
        public boolean enable = false;
        public Seq<FactorIO<?>> factors = new Seq<>();

        public FactorBucket(String name){
            this.name = name;
        }

        public FactorBucket(String name, FactorIO<?>... io){
            this(name);
            addAll(io);
        }

        public void add(FactorIO<?> factor){
            factors.add(factor);
        }

        public void addAll(FactorIO<?>[] factor){
            factors.add(factor);
        }

        public void build(Table table){
            var enb = new TextButton(name, Styles.flatTogglet);
            enb.clicked(() -> {
                enable = !enable;
                enb.setChecked(enable);
            });
            enb.setChecked(enable);
            table.add(enb).grow();
            var expand = table.button("" + Iconc.downOpen, Styles.flatTogglet, () -> {}).size(32f).get();
            expand.setChecked(enable);
            table.row();
            table.collapser(t -> {
                factors.each(fac -> {
                    t.table(fac::build);
                    t.row();
                });
            }, expand::isChecked).colspan(2);
        }
    }

    public static class SourceIOEntity extends IOEnitiy{
        public final static SourceIOEntity source = new SourceIOEntity(Blocks.itemSource);
        public static BaseDialog select = new BaseDialog("@ui.selectfactor.title"){{addCloseButton();}};
        public static BaseDialog iconSelect = new BaseDialog("@ui.selectfactor.title"){{addCloseButton();}};
        public boolean needRebuild = false;
        protected static SourceIOEntity target;

        public static void init(){
            select.cont.clear();
            select.cont.pane(p -> {
                final int[] i = {0};
                FactorIO.factors.each((obj, factor) -> {
                    p.button(b -> factor.buildIcon(b, true), Styles.flatt, () -> {
                        if(target != null){
                            target.add(factor.copy());
                            target.needRebuild = true;
                        }
                        select.hide();
                    }).pad(6f).size(196f, 32f);
                    if(Mathf.mod(++i[0], 6) == 0) p.row();
                });
            }).grow().with(p -> {
                p.setForceScroll(true, true);
            });

            iconSelect.cont.clear();
            iconSelect.cont.pane(p -> {
                final int[] i = {0};
                Vars.content.each(c -> {
                    if(c instanceof UnlockableContent uc){
                        p.button(b -> b.image(uc.uiIcon), Styles.flatt, () -> {
                            if(target != null){
                                target.content = uc;
                                target.needRebuild = true;
                            }
                            iconSelect.hide();
                        }).pad(6f).size(32f, 32f);
                        if(Mathf.mod(++i[0], 16) == 0) p.row();
                    }
                });
            }).grow().with(p -> {
                p.setForceScroll(true, true);
            });
        }

        public SourceIOEntity(UnlockableContent type){
            super(type);
        }

        @Override
        public void buildFactors(Table table){
            table.clear();
            table.table(t -> {
                t.button("" + Iconc.image, Styles.cleart, () -> {
                    target = this;
                    iconSelect.show();
                }).minSize(32f).grow();
                t.button("" + Iconc.add, Styles.cleart, () -> {
                    target = this;
                    select.show();
                }).minSize(32f).grow();

                t.button("" + Iconc.cancel, Styles.cleart, () -> {
                    factors.clear();
                    buildFactors(table);
                }).size(32f);
            }).growX().update(t -> {
                if(needRebuild){
                    buildFactors(table);
                    needRebuild = false;
                }
            });

            table.row();

            factors.each(fac -> {
                table.table(fac::build);
                table.button("" + Iconc.cancel, Styles.cleart, () -> {
                    factors.remove(fac);
                    buildFactors(table);
                }).size(16f);
                table.row();
            });
        }

        @Override
        public SourceIOEntity copy(){
            var copy = new SourceIOEntity(this.content);
            this.factors.each(c -> copy.add(c.copy()));
            return copy;
        }
    }

    /**Default factors for each unit.*/
    public static Seq<IOEnitiy> defaults = new Seq<>();
    /**Functions generating default IO, with name id. Removable and re-run generation. Execution order configurable with OrderedMap.orderedkeys(). */
    public static OrderedMap<String, Cons<IOEnitiy>> initruns = new OrderedMap<>();

    public static void init(){
        float timemul = 60f;

        initruns.put("SP-TimedFactory", e -> {
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
                if(block instanceof OverdriveProjector op) ticks = op.useTime;
                if(block instanceof MendProjector mp) ticks = mp.useTime;
                if(block instanceof ForceProjector fp) ticks = fp.phaseUseTime;

                for(var cons : block.consumers){
                    if(cons instanceof ConsumePower cp) e.add(new CustomIO("power", -cp.usage * timemul, !cons.booster));
                    //craft ticks only supports some building consumers
                    if(ticks <= 100000000f && cons instanceof ConsumeItems ci){
                        for(var i : ci.items) e.add(new ItemIO(i.item, -i.amount / ticks * timemul, !cons.booster));
                    }
                    if(cons instanceof ConsumeLiquid cl) e.add(new LiquidIO(cl.liquid, -cl.amount * timemul, !cons.booster));
                    if(cons instanceof ConsumeLiquids cl){
                        var bucket = new FactorBucket(Iconc.liquid + "");
                        for(var stack : cl.liquids){
                            bucket.add(new LiquidIO(stack.liquid, -stack.amount * timemul, !cl.booster));
                        }
                        bucket.enable = !cl.booster;
                        e.add(bucket);
                    }
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
                    if(pg instanceof ConsumeGenerator cg){
                        if(cg.filterItem != null){
                            for(var item : Vars.content.items()){
                                if(!cg.filterItem.filter.get(item)) continue;
                                float power = 1f;
                                if(cg.filterItem instanceof ConsumeItemFlammable) power = item.flammability;
                                if(cg.filterItem instanceof ConsumeItemRadioactive) power = item.radioactivity;
                                if(cg.filterItem instanceof ConsumeItemExplosive) power = item.explosiveness;
                                if(cg.filterItem instanceof ConsumeItemCharged) power = item.charge;
                                e.add(new FactorBucket(item.localizedName,
                                        new CustomIO("power", power * cg.powerProduction * timemul, true),
                                        new ItemIO(item, -1f / ticks * timemul, true)
                                ));
                            }
                        }else{
                            e.add(new CustomIO("power", cg.powerProduction * timemul, true));
                        }

                        if(cg.outputsLiquid && cg.outputLiquid != null){
                            e.add(new LiquidIO(cg.outputLiquid.liquid, cg.outputLiquid.amount * timemul, true));
                        }
                    }else if(block instanceof ThermalGenerator tg){
                        if(tg.outputsLiquid && tg.outputLiquid != null){
                            e.add(new LiquidIO(tg.outputLiquid.liquid, tg.outputLiquid.amount * timemul * tg.size * tg.size, true));
                        }
                        e.add(new CustomIO("power", pg.powerProduction * timemul * tg.size * tg.size, true));
                    }else{
                        e.add(new CustomIO("power", pg.powerProduction * timemul, true));
                    }
                }else if(block instanceof Reconstructor r){
                    for(UnitType[] uta : r.upgrades){
                        if(uta == null || uta[0] == null || uta[1] == null) continue;
                        e.add(new FactorBucket(uta[1].localizedName,
                                new UnitIO(uta[0], -1f / ticks * timemul, true),
                                new UnitIO(uta[1], 1f / ticks * timemul, true)
                        ));
                    }
                }
            }
        });

        initruns.put("SP-UnitFactory", e -> {
            if(e.content instanceof UnitFactory uf){
                uf.plans.each(plan -> {
                    var bucket = new FactorBucket(plan.unit.localizedName, new UnitIO(plan.unit, 1f / plan.time * timemul, true));
                    for(var stack : plan.requirements){
                        bucket.add(new ItemIO(stack.item, -stack.amount / plan.time * timemul, true));
                    }
                    e.add(bucket);
                });
            }

            if(e.content instanceof UnitAssembler ua){
                ua.plans.each(plan -> {
                    var bucket = new FactorBucket(plan.unit.localizedName, new UnitIO(plan.unit, 1f / plan.time * timemul, true));
                    for(var stack : plan.requirements){
                        if(stack.item instanceof Block t) bucket.add(new BlockIO(t, -stack.amount / plan.time * timemul, true));
                        if(stack.item instanceof UnitType t) bucket.add(new UnitIO(t, -stack.amount / plan.time * timemul, true));
                    }
                    e.add(bucket);
                });
            }
        });

        initruns.put("SP-Turret", e -> {
            if(e.content instanceof ReloadTurret rt){
                if(rt.coolant instanceof ConsumeCoolant cc){
                    Vars.content.liquids().each(liquid -> {
                        if(liquid.coolant && !liquid.gas && liquid.temperature <= cc.maxTemp && liquid.flammability < cc.maxFlammability) e.add(new LiquidIO(liquid, -rt.coolant.amount * timemul, false));
                    });
                }

                if(e.content instanceof ItemTurret it){
                    it.ammoTypes.each((item, bullet) -> {
                        var bucket = new FactorBucket(item.localizedName);
                        bucket.add(new ItemIO(item, -1f / bullet.ammoMultiplier / it.reload * timemul * bullet.reloadMultiplier, true));
                        bucket.add(new CustomIO("damage", bullet.estimateDPS() / it.reload * timemul * bullet.reloadMultiplier, true));
                        e.add(bucket);
                    });
                }
            }
        });

        initruns.put("SP-BlockSize", e -> {
            if(e.content instanceof Block b){
                var f = new CustomIO("size", b.size * b.size, true);
                f.continuous = false;
                e.add(f);
            }
        });

        reGenerateDefaults();
        SourceIOEntity.init();

        Events.on(EventType.ContentInitEvent.class, e -> {
            reGenerateDefaults();
            SourceIOEntity.init();
        });
    }

    public static void reGenerateDefaults(){
        Vars.content.blocks().each(block -> {
            var entity = get(block);
            entity.factors.clear();
            initruns.orderedKeys().each(name -> initruns.get(name).get(entity));
        });

        Vars.content.items().each(type -> FactorIO.factors.put(type, new ItemIO(type, 0f, true)));
        Vars.content.liquids().each(type -> FactorIO.factors.put(type, new LiquidIO(type, 0f, true)));
        Vars.content.units().each(type -> FactorIO.factors.put(type, new UnitIO(type, 0f, true)));
        Vars.content.blocks().each(type -> {
            if(!type.hasBuilding()) return;
            FactorIO.factors.put(type, new BlockIO(type, 0f, true));
        });
        FactorIO.factors.put("power", new CustomIO("power", 0f, true));
        FactorIO.factors.put("damage", new CustomIO("damage", 0f, true));
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
