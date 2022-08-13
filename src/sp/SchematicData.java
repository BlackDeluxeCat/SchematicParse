package sp;

import arc.func.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.Block;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.consumers.ConsumeLiquids;
import mindustry.world.modules.LiquidModule;

import java.util.Arrays;

import static mindustry.Vars.*;
import static sp.SchematicParse.consTextB;

public class SchematicData{
    //方块的默认配置，参与使用，创建自定义配置要从默认配置copy副本。
    public static ConsumeConfig[] defaults;

    public Schematic sche;
    public int[] blocks;
    public Seq<ConsumeConfig>[] configs;
    public Runnable update;

    public float[] items = new float[content.items().size];
    public float[] liquids = new float[content.liquids().size];

    public SchematicData(){
        blocks = new int[content.blocks().size];
        configs = new Seq[content.blocks().size];
        defaults = new ConsumeConfig[content.blocks().size];
    }

    public void read(Schematic schematic){
        sche = schematic;
        clear();

        for(var tile : sche.tiles){
            blocks[tile.block.id] += 1;
        }

        update();

        sche = null;
    }

    public void clear(){
        Arrays.fill(blocks, 0);
        for(int i = 0; i < configs.length; i++){
            if(configs[i] == null){
                configs[i] = new Seq<>();
            }else{
                configs[i].clear();
            }
            configs[i].add(defaults[i]);
        }
    }

    public void update(){
        updateAmount();

        for(var seq : configs){
            if(seq != null) seq.each(ConsumeConfig::update);
        }

        Arrays.fill(items, 0f);
        Arrays.fill(liquids, 0f);

        for(var seq : configs){
            seq.each(con -> {
                for(int i = 0; i < items.length; i++) items[i] += con.getItem(i);
                for(int i = 0; i < liquids.length; i++) liquids[i] += con.getLiquid(i);
            });
        }

        if(update != null) update.run();
    }

    public void updateAmount(){
        for(int i = 0; i < configs.length; i++){
            if(configs[i] == null) continue;

            if(countCfg(i) > blocks[i]){
                for(var config : configs[i]){
                    if(countCfg(i) <= blocks[i]) break;
                    config.amount = 0;
                }
            }

            configs[i].first().amount = blocks[i] - countCfg(i);
        }
    }

    public int countCfg(int blockId){
        return configs[blockId] == null ? 0 : configs[blockId].sum(cfg -> cfg.isDefault ? 0 : cfg.amount);
    }

    public void setDefaults(){
        for(var block : content.blocks()) {
            var seq = new Seq<ConsumeSet>();
            float[] items2 = new float[items.length];
            float[] liquids2 = new float[liquids.length];

            if(block instanceof Drill drill){
                for (var b : content.blocks()){
                    if (b instanceof Floor floor){
                        var item = floor.itemDrop;
                        if (item == null) continue;
                        items2[item.id] = item.hardness > drill.tier ? 0f : drill.size * drill.size / ((drill.drillTime + item.hardness * drill.hardnessDrillMultiplier) / 60f);
                    }
                }
                seq.add(new DynamicConsumeSet("Base Out", items2, liquids2, null, null));

                for(var b : content.blocks()){
                    if(b instanceof Floor floor){
                        var item = floor.itemDrop;
                        if (item == null) continue;
                        items2[item.id] = item.hardness > drill.tier ? 0f : drill.size * drill.size * drill.liquidBoostIntensity * drill.liquidBoostIntensity / ((drill.drillTime + item.hardness * drill.hardnessDrillMultiplier) / 60f);
                    }
                }

                var liq = drill.findConsumer(c -> c instanceof ConsumeLiquid);
                if(liq instanceof ConsumeLiquid liqc) liquids2[liqc.liquid.id] = -liqc.amount * 60f;

                seq.add(new DynamicConsumeSet("Boost Out", items2, liquids2, null, null));

            }else if(block instanceof GenericCrafter gb){
                if(gb.outputItems != null){
                    for(var out : gb.outputItems) items2[out.item.id] += out.amount / (gb.craftTime/60f);
                }

                var consi = block.findConsumer(c -> c instanceof ConsumeItems);
                if(consi instanceof ConsumeItems ci){
                    for(var stack : ci.items) items2[stack.item.id] -= stack.amount / (gb.craftTime/60f);
                }

                if(gb.outputLiquids != null){
                    for(var out : gb.outputLiquids) liquids2[out.liquid.id] += out.amount*60f;
                }

                var consl = block.findConsumer(c -> c instanceof ConsumeLiquid);
                if(consl instanceof ConsumeLiquid ci){
                    liquids2[ci.liquid.id] -= ci.amount*60f;
                }

                var consls = block.findConsumer(c -> c instanceof ConsumeLiquids);
                if(consls instanceof ConsumeLiquids ci){
                    for(var stack : ci.liquids) liquids2[stack.liquid.id] -= stack.amount*60f;
                }

                seq.add(new ConsumeSet("Craft", items2, liquids2));

            }else if(block instanceof SolidPump solidPump){
                liquids2[solidPump.result.id] += solidPump.pumpAmount*60f;

                var consls = block.findConsumer(c -> c instanceof ConsumeLiquids);
                if(consls instanceof ConsumeLiquids ci){
                    for(var stack : ci.liquids) liquids2[stack.liquid.id] -= stack.amount*60f;
                }

                var consl = block.findConsumer(c -> c instanceof ConsumeLiquid);
                if(consl instanceof ConsumeLiquid ci){
                    liquids2[ci.liquid.id] -= ci.amount*60f;
                }

                if(block instanceof Fracker fracker){
                    var consi = block.findConsumer(c -> c instanceof ConsumeItems);
                    if(consi instanceof ConsumeItems ci){
                        for(var stack : ci.items) items2[stack.item.id] -= stack.amount / (fracker.itemUseTime/60f);
                    }
                }

                seq.add(new ConsumeSet("Pump", items2, liquids2));
            }else if(block instanceof Pump pump){
                for(var liquid : content.liquids()){
                    liquids2[liquid.id] += pump.size * pump.size * pump.pumpAmount * 60f;
                }

                seq.add(new DynamicConsumeSet("Pump", items2, liquids2, null, null));
            }

            var cfg = new ConsumeConfig(block, seq);
            defaults[block.id] = cfg;
        }
    }

    /** {@link ConsumeConfig}类，为某种方块添加一套计算用的消耗配置。
     * 在游戏初始化时为每个block创建默认配置，其中有些消耗模块是默认关闭的。
     * 当你需要在蓝图解析中新建自定义配置，就从defaults列表clone获得副本，当你需要覆盖某个block的默认config，就修改其{@link #cons}，可定义新的{@link ConsumeConfig}子类。*/
    //TODO 增加灵活的自定义倍率
    public class ConsumeConfig{
        /**
         * 使用该配置的方块数量。配置均>=0
         */
        public int amount = 0;
        public boolean isDefault;
        public Block block;
        /**
         * 方块的消耗能力配置。创建ConsumeConfig副本时，对每个ConsumeSet做copy
         */
        public Seq<ConsumeSet> cons;

        public float[] items = new float[content.items().size];
        public float[] liquids = new float[content.liquids().size];

        public float efficiency = 1f;

        public ConsumeConfig(Block block, @Nullable Seq<ConsumeSet> cons, boolean isDefault){
            this.cons = cons;
            this.block = block;
            this.isDefault = isDefault;
        }

        public ConsumeConfig(Block block, @Nullable Seq<ConsumeSet> cons){
            this(block, cons, true);
        }

        public ConsumeConfig copy(){
            var seq = new Seq<ConsumeSet>();
            cons.each(c -> seq.add(c.copy()));
            var cfg = new ConsumeConfig(block, seq, false);
            cfg.amount = 0;
            return cfg;
        }

        public void update(){
            Arrays.fill(items, 0f);
            Arrays.fill(liquids, 0f);
            cons.each(con -> {
                con.update();
                for (int i = 0; i < items.length; i++) items[i] += con.enabled ? con.getItem(i) : 0f;
                for (int i = 0; i < liquids.length; i++) liquids[i] += con.enabled ? con.getLiquid(i) : 0f;
            });
        }

        //seems useless
        public float getItem(int id){
            return items[id] * efficiency * amount;
        }

        public float getLiquid(int id){
            return liquids[id] * efficiency * amount;
        }

        /**
         * build block config table.
         */
        public void buildBlockConfig(Table table){
            table.table(t -> {
                t.add("Mul");
                t.field(String.valueOf(efficiency), TextField.TextFieldFilter.floatsOnly, s -> {
                    efficiency = Strings.parseFloat(s, 1f);
                    update();
                }).width(80f).get().setMessageText("Default: x1.00");
                t.button("-", Styles.flatt, () -> {
                    amount = Mathf.clamp(--amount, 0, blocks[block.id]);
                    SchematicData.this.update();
                }).with(consTextB).width(48f).disabled(isDefault);
                t.button("+", Styles.flatt, () -> {
                    amount = Mathf.clamp(++amount, 0, blocks[block.id]);
                    SchematicData.this.update();
                }).with(consTextB).width(48f).disabled(isDefault);
            });
            table.row();
            buildEntry(table);
        }

        /**
         * build entry config table.
         */
        public void buildEntry(Table table){
            if(cons == null)return;
            for(var con : cons){
                table.table(con::build);
                table.row();
            }
        }
    }

    /** {@link ConsumeSet}是方块的消耗模块，每一个模块提供给方块某种消耗能力。 */
    public class ConsumeSet{
        public String name;
        public boolean enabled = true;

        /** Basic stats of consume set. Don't change it. */
        public float[] items, liquids;

        public ConsumeSet(String name, float[] items, float[] liquids){
            this.name = name;
            this.items = Arrays.copyOf(items, items.length);
            this.liquids = Arrays.copyOf(liquids, liquids.length);
        }

        public ConsumeSet copy(){
            var newSet = new ConsumeSet(name, items, liquids);
            newSet.enabled = enabled;
            return newSet;
        }

        public void update(){}

        public float getItem(int id){
            return items[id];
        }

        public float getLiquid(int id){
            return liquids[id];
        }

        public void build(Table table){
            table.table(t -> {
                if(name != null) t.add(name).colspan(2);
                int index = 0;
                for(int i = 0; i < items.length; i++){
                    if(Mathf.zero(items[i])) continue;
                    if(Mathf.mod(index++, 8) == 0) t.row();
                    var label = new Label(Strings.autoFixed(items[i], 2));
                    label.setFontScale(0.8f);
                    label.setAlignment(Align.bottomRight);
                    label.setFillParent(true);
                    t.stack(new Image(content.item(i).uiIcon), label).size(32f);
                }

                for(int i = 0; i < liquids.length; i++){
                    if(Mathf.zero(liquids[i])) continue;
                    if(Mathf.mod(index++, 8) == 0) t.row();
                    var label = new Label(Strings.autoFixed(liquids[i], 2));
                    label.setFontScale(0.8f);
                    label.setAlignment(Align.bottomRight);
                    label.setFillParent(true);
                    t.stack(new Image(content.liquid(i).uiIcon), label).size(32f);
                }
            });
        }

    }

    /** DynamicConsumeSet模块具有可调整的消耗能力，可以指定消耗的物品、液体id，将{@link #items}{@link #liquids}作为备选列表。 */
    public class DynamicConsumeSet extends ConsumeSet{
        public int itemId = -1;
        public int liquidId = -1;
        public Boolf<Item> itemFilter;
        public Boolf<Liquid> liquidFilter;

        public DynamicConsumeSet(String name, float[] items, float[] liquids, Boolf<Item> itemFilter, Boolf<Liquid> liquidFilter){
            super(name, items, liquids);
            this.itemFilter = itemFilter;
            this.liquidFilter = liquidFilter;
        }

        @Override
        public float getItem(int id) {
            return id >= 0 && id == itemId ? super.getItem(id) : 0f;
        }

        @Override
        public float getLiquid(int id) {
            return id >= 0 && id == liquidId ? super.getLiquid(id) : 0f;
        }

        @Override
        public void build(Table table){
            table.table(t -> {
                int index = 0;
                for(int i = 0; i < items.length; i++){
                    if((itemFilter != null && !itemFilter.get(content.item(i))) || Mathf.zero(items[i])) continue;
                    if(Mathf.mod(index++, 8) == 0) t.row();
                    var label = new Label(Strings.autoFixed(items[i], 2));
                    label.setFontScale(0.8f);
                    label.setAlignment(Align.bottomRight);
                    label.setFillParent(true);
                    var item = content.item(i);
                    t.button(b -> b.stack(new Image(item.uiIcon), label), Styles.flatTogglet, () -> {
                        itemId = itemId == item.id ? -1 : item.id;
                        SchematicData.this.update();
                    }).checked(b -> itemId == item.id).size(32f);
                }

                for(int i = 0; i < liquids.length; i++){
                    if((liquidFilter != null && !liquidFilter.get(content.liquid(i))) || Mathf.zero(liquids[i])) continue;
                    if(Mathf.mod(index++, 8) == 0) t.row();
                    var label = new Label(Strings.autoFixed(liquids[i], 2));
                    label.setFontScale(0.8f);
                    label.setAlignment(Align.bottomRight);
                    label.setFillParent(true);
                    var item = content.liquid(i);
                    t.button(b -> b.stack(new Image(item.uiIcon), label), Styles.flatTogglet, () -> {
                        liquidId = liquidId == item.id ? -1 : item.id;
                        SchematicData.this.update();
                    }).checked(b -> liquidId == item.id).size(32f);
                }
            });
        }

        @Override
        public DynamicConsumeSet copy(){
            var newSet = new DynamicConsumeSet(name, items, liquids, itemFilter, liquidFilter);
            newSet.itemId = itemId;
            newSet.liquidId = liquidId;
            return newSet;
        }
    }
}
