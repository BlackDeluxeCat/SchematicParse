package sp;

import arc.func.*;
import arc.struct.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;

import java.util.Arrays;

import static mindustry.Vars.*;

//TODO will rewrite in bit operation one day.
public class SchematicData{
    public Schematic sche;

    public static Seq<Cons2<Block, SchematicData>> blockHandlers = new Seq<>();
    public int[] blocks;
    /** item-block-amount : float, item-block : boolean */
    public float[][] items;
    public boolean[][] itemsSign;
    public boolean[][] itemsDisplay;
    /** liquid-block-amount : float, liquid-block : boolean */
    public float[][] liquids;
    public boolean[][] liquidsSign;
    public boolean[][] liquidsDisplay;

    public SchematicData(){}

    public float getItem(int id){
        float amount = 0f;
        for(int i = 0; i < items[id].length; i++){
            amount += getItemSign(id, i) ? items[id][i] : 0f;
        }
        return amount;
    }

    public boolean getItemSign(int itemId, int blockId){
        if(itemId >= itemsSign.length || blockId >= itemsSign[itemId].length) return false;
        return itemsSign[itemId][blockId];
    }

    public boolean getItemDisplay(int itemId, int blockId){
        if(blockId == -1){
            for(boolean bool : itemsDisplay[itemId]){
                if(bool) return true;
            }
            return false;
        }
        if(itemId >= itemsDisplay.length || blockId >= itemsDisplay[itemId].length) return false;
        return itemsDisplay[itemId][blockId];
    }

    public void putItem(int itemId, int blockId, float amount){
        items[itemId][blockId] += amount;
        itemsDisplay[itemId][blockId] = true;
        itemsSign[itemId][blockId] = true;
    }

    public void putItemSign(int itemId, int blockId, boolean sign){
        itemsSign[itemId][blockId] = sign;
    }

    public float getLiquid(int id){
        float amount = 0f;
        for(int i = 0; i < liquids[id].length; i++){
            amount += getLiquidSign(id, i) ? liquids[id][i] : 0f;
        }
        return amount;
    }

    public boolean getLiquidSign(int itemId, int blockId){
        if(itemId >= liquidsSign.length || blockId >= liquidsSign[itemId].length) return false;
        return liquidsSign[itemId][blockId];
    }

    public boolean getLiquidDisplay(int itemId, int blockId){
        if(blockId == -1){
            for(boolean bool : liquidsDisplay[itemId]){
                if(bool) return true;
            }
            return false;
        }
        if(itemId >= liquidsDisplay.length || blockId >= liquidsDisplay[itemId].length) return false;
        return liquidsDisplay[itemId][blockId];
    }

    public void putLiquid(int itemId, int blockId, float amount){
        liquids[itemId][blockId] += amount;
        liquidsDisplay[itemId][blockId] = true;
        liquidsSign[itemId][blockId] = true;
    }

    public void putLiquidSign(int itemId, int blockId, boolean sign){
        liquidsSign[itemId][blockId] = sign;
    }

    public boolean getSign(int blockId){
        for(int i = 0; i < itemsSign.length; i++){
            if(getItemDisplay(i, blockId) && getItemSign(i, blockId)) return true;
        }

        for(int i = 0; i < liquidsSign.length; i++){
            if(getLiquidDisplay(i, blockId) && getLiquidSign(i, blockId)) return true;
        }
        return false;
    }

    public void putSign(int blockId, boolean sign){
        for(int i = 0; i < itemsSign.length; i++){
            putItemSign(i, blockId, sign);
        }

        for(int i = 0; i < liquidsSign.length; i++){
            putLiquidSign(i, blockId, sign);
        }
    }

    public void reset(){
        blocks = new int[content.blocks().size];
        items = new float[content.items().size][content.blocks().size];
        itemsSign = new boolean[content.items().size][content.blocks().size];
        itemsDisplay = new boolean[content.items().size][content.blocks().size];
        liquids = new float[content.liquids().size][content.blocks().size];
        liquidsSign = new boolean[content.liquids().size][content.blocks().size];
        liquidsDisplay = new boolean[content.liquids().size][content.blocks().size];
    }

    public void clear(){
        Arrays.fill(blocks, 0);
        for(var itemsArr : items) Arrays.fill(itemsArr, 0f);
        for(var itemsArr : liquids) Arrays.fill(itemsArr, 0f);
        for(var itemsArr : itemsSign) Arrays.fill(itemsArr, false);
        for(var itemsArr : itemsDisplay) Arrays.fill(itemsArr, false);
        for(var itemsArr : liquidsSign) Arrays.fill(itemsArr, false);
        for(var itemsArr : liquidsDisplay) Arrays.fill(itemsArr, false);
    }

    public void read(Schematic schematic){
        sche = schematic;
        clear();

        for(var tile : sche.tiles){
            blocks[tile.block.id] += 1;
        }

        for(var block : content.blocks()){
            if(blocks[block.id] <= 0) continue;
            blockHandlers.each(handler -> handler.get(block, this));
        }

        sche = null;
    }

    public static void initHandlers(){
        blockHandlers.add((block, data) -> {
            if(block instanceof GenericCrafter gb){
                var consi = block.findConsumer(c -> c instanceof ConsumeItems);
                if(consi instanceof ConsumeItems ci){
                    for(var stack : ci.items) data.putItem(stack.item.id, gb.id, -data.blocks[block.id] * stack.amount / (gb.craftTime / 60f));
                }

                if(gb.outputItems != null){
                    for(var stack : gb.outputItems) data.putItem(stack.item.id, gb.id, data.blocks[block.id] * stack.amount / (gb.craftTime / 60f));
                }

                var conss = block.findConsumer(c -> c instanceof ConsumeLiquids);
                if(conss instanceof ConsumeLiquids cl){
                    for(var stack : cl.liquids) data.putLiquid(stack.liquid.id, gb.id, -data.blocks[block.id] * stack.amount * 60f);
                }

                var cons = block.findConsumer(c -> c instanceof ConsumeLiquid);
                if(cons instanceof ConsumeLiquid cl) data.putLiquid(cl.liquid.id, gb.id, -data.blocks[block.id] * cl.amount * 60f);

                if(gb.outputLiquids != null){
                    for(var stack : gb.outputLiquids) data.putLiquid(stack.liquid.id, gb.id, data.blocks[block.id] * stack.amount * 60f);
                }
            }
        });

        blockHandlers.add((block, data) -> {
            if(block instanceof Fracker f){
                var consi = block.findConsumer(c -> c instanceof ConsumeItems);
                if(consi instanceof ConsumeItems ci){
                    for(var stack : ci.items) data.putItem(stack.item.id, f.id, -data.blocks[block.id] * stack.amount / (f.itemUseTime / 60f));
                }
            }

            if(block instanceof SolidPump sp){
                var conss = block.findConsumer(c -> c instanceof ConsumeLiquids);
                if(conss instanceof ConsumeLiquids cl){
                    for(var stack : cl.liquids) data.putLiquid(stack.liquid.id, sp.id, -data.blocks[block.id] * stack.amount * 60f);
                }

                var cons = block.findConsumer(c -> c instanceof ConsumeLiquid);
                if(cons instanceof ConsumeLiquid cl) data.putLiquid(cl.liquid.id, sp.id, -data.blocks[block.id] * cl.amount * 60f);

                if(sp.result != null){
                    data.putLiquid(sp.result.id, sp.id, data.blocks[block.id] * sp.pumpAmount * 60f);
                }
            }
        });
    }
}
