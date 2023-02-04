package sp.struct;

import arc.func.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
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
    public Schematic sche;
    public boolean logicparsed = false;
    public boolean calculatorimported = false;
    public ObjectIntMap<UnlockableContent> used = new ObjectIntMap<>();

    public void read(Schematic schematic){
        used.clear();
        sche = schematic;
        for(var tile : sche.tiles){
            used.put(tile.block, used.get(tile.block, 0) + 1);
        }
        logicparsed = false;
        calculatorimported = false;
    }
}
