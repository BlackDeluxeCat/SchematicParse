package sp;

import arc.*;
import arc.func.Cons;
import arc.func.Floatf;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.Block;
import mindustry.world.blocks.heat.HeatProducer;
import mindustry.world.blocks.logic.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import static mindustry.Vars.*;

public class SchematicParse extends Mod{
    public static float tmpFloat = 0f;
    public static int tmpIndex = 0, tmpIndex2 = 0;
    public static SchematicData data;

    public static Table blocksTable, cfgTable, statsTable;
    public static BaseDialog settingDialog;
    public static Cons<TextButton> consTextB = c -> {
        c.getLabel().setAlignment(Align.center);
        c.getLabel().setWrap(false);
        c.margin(6f);
    };

    public SchematicParse(){
        Log.info("SchematicParse Ready For Rush");
        cfgTable = new Table();
        statsTable = new Table();
        //listen for game load event
        Events.on(ClientLoadEvent.class, e -> {
            data = new SchematicData();
            data.setDefaults();
            schelogic();
        });
    }

    @Override
    public void loadContent(){
        Log.info("Loading some example content.");
    }

    public static void schelogic(){
        SchematicsDialog.SchematicInfoDialog info = Reflect.get(SchematicsDialog.class, ui.schematics, "info");
        info.shown(Time.runTask(10f, () -> {
            settingDialog = new BaseDialog("Schematic Parse Setting");
            settingDialog.addCloseButton();

            Label l = info.find(e -> e instanceof Label ll && ll.getText().toString().contains("[[" + Core.bundle.get("schematic") + "] "));
            if(l != null){
                String schename = l.getText().toString().replace("[[" + Core.bundle.get("schematic") + "] ", "");
                Schematic sche = schematics.all().find(s -> s.name().equals(schename));
                if(sche != null && sche.width <= maxSchematicSize && sche.height <= maxSchematicSize){
                    data.read(sche);
                    info.cont.row();
                    info.cont.table(spt -> {
                        spt.name = "ScheParseTable";
                        spt.table(t -> {
                            t.defaults().uniform().fill();
                            t.button("" + Iconc.zoom + Iconc.blockMicroProcessor + Iconc.blockMessage, Styles.flatt, () -> {}).with(b -> {
                                b.getLabel().setWrap(false);
                                b.clicked(() -> {
                                    b.setDisabled(true);
                                    SchematicsDialog.SchematicImage image = info.find(e -> e instanceof SchematicsDialog.SchematicImage);
                                    if(image == null){
                                        Log.infoTag("SchematicPrase", "Failed to get Sche Image, skip.");
                                        return;
                                    }
                                    Vec2 imagexy = Tmp.v1;
                                    imagexy.set(0f, 0f);
                                    image.localToParentCoordinates(imagexy);
                                    sche.tiles.each(tile -> {
                                        int size = tile.block.size;
                                        float padding = 2f;
                                        float bufferScl = Math.min(image.getWidth() / ((sche.width + padding) * 32f * Scl.scl()), image.getHeight() / ((sche.height + padding) * 32f * Scl.scl()));
                                        Vec2 tablexy = new Vec2(tile.x + tile.block.sizeOffset, tile.y + tile.block.sizeOffset);
                                        tablexy.add(-sche.width/2f, -sche.height/2f);
                                        tablexy.scl(32f * Scl.scl());
                                        tablexy.scl(bufferScl);
                                        tablexy.add(imagexy.x, imagexy.y);
                                        tablexy.add(image.getWidth()/2f, image.getHeight()/2f);
                                        if(tile.block instanceof LogicBlock){
                                            //tile.config is a byte[] including compressed code and links
                                            try(DataInputStream stream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream((byte[])tile.config)))){
                                                stream.read();
                                                int bytelen = stream.readInt();
                                                if(bytelen > 1024 * 500) throw new IOException("Malformed logic data! Length: " + bytelen);
                                                byte[] bytes = new byte[bytelen];
                                                stream.readFully(bytes);
                                                TextButton bl = new TextButton("" + Iconc.paste);
                                                bl.setStyle(Styles.flatt);
                                                bl.clicked(() -> {
                                                    Core.app.setClipboardText(new String(bytes, charset));
                                                });
                                                info.cont.addChild(bl);
                                                bl.setPosition(tablexy.x, tablexy.y);
                                                bl.setSize(Scl.scl() * 36f * (size <= 1f ? 0.5f:1f));
                                            }catch(Exception ignored){
                                                //invalid logic doesn't matter here
                                            }
                                        }
                                        if(tile.block instanceof MessageBlock){
                                            TextButton bl = new TextButton("" + Iconc.paste);
                                            bl.setStyle(Styles.flatt);
                                            bl.clicked(() -> {
                                                Core.app.setClipboardText(tile.config.toString());
                                            });
                                            bl.addListener(new Tooltip(tooltip -> {
                                                tooltip.background(Styles.black5);
                                                tooltip.add(tile.config.toString());
                                            }));
                                            info.cont.addChild(bl);
                                            bl.setPosition(tablexy.x, tablexy.y);
                                            bl.setSize(Scl.scl() * 36f * (size <= 1f ? 0.5f:1f));
                                        }
                                    });
                                });
                            }).height(40f).with(consTextB);

                            t.row();

                            t.button("" + Iconc.settings, Styles.flatt, () -> settingDialog.show()).height(32f).with(consTextB);
                        });

                        rebuildStats();

                        spt.pane(statsTable).with(p -> {
                            p.setupFadeScrollBars(0.3f, 0.3f);
                            p.setFadeScrollBars(true);
                        });
                    }).maxHeight(200f).growX();
                }
            }

            tmpIndex = 0;
            settingDialog.cont.pane(p -> {
                blocksTable = p;
                p.defaults().uniform().pad(1f);
                p.setBackground(Styles.grayPanel);
                for(var block : content.blocks()){
                    if(data.blocks[block.id] <= 0) continue;
                    if(Mathf.mod(tmpIndex++, 8) == 0) p.row();
                    var label = new Label("" + data.blocks[block.id]);
                    label.setAlignment(Align.bottomRight);
                    label.setFillParent(true);
                    var image = new Image(block.uiIcon);
                    image.setFillParent(true);
                    p.button(b -> b.stack(image, label).fill(), Styles.clearNonei, () -> rebuildContentSelect(block)).size(48f);
                }
            }).with(p -> {
                p.setupFadeScrollBars(0.3f, 0.3f);
                p.setFadeScrollBars(true);
            }).maxHeight(400f);

            settingDialog.cont.row();

            rebuildContentSelect(null);

            settingDialog.cont.pane(cfgTable).with(p -> {
                p.setupFadeScrollBars(0.3f, 0.3f);
                p.setFadeScrollBars(true);
            }).grow();
        }));
    }

    public static void rebuildStats(){
        statsTable.clear();
        statsTable.setBackground(Styles.grayPanel);
        statsTable.margin(4f);
        statsTable.defaults().uniform().left().pad(1f).fill();

        tmpIndex = 0;
        for(var item : content.items()){
            if(Mathf.zero(data.items[item.id])) continue;
            statsTable.label(() -> {
                float amt = data.items[item.id];
                return item.emoji() + (amt > 0f ? "+" : "") + Strings.autoFixed(amt, 2);
            });
            if(Mathf.mod(tmpIndex++, 4) == 3) statsTable.row();
        }

        for(var item : content.liquids()){
            if(Mathf.zero(data.liquids[item.id])) continue;
            statsTable.label(() -> {
                float amt = data.liquids[item.id];
                return item.emoji() + (amt > 0f ? "+" : "") + Strings.autoFixed(amt, 2);
            });
            if(Mathf.mod(tmpIndex++, 4) == 3) statsTable.row();
        }

        statsTable.add(Iconc.blockHeatSource + "+" + Strings.fixed(cumsum(block -> block instanceof HeatProducer b ? b.heatOutput * data.blocks[block.id] : 0f), 1));
        statsTable.row();
    }

    public static void rebuildContentSelect(@Nullable Block block){
        cfgTable.clear();
        if(block == null) return;
        cfgTable.table(t -> {
            t.image(block.uiIcon).size(16f);
            t.add(block.localizedName);
            t.button("" + Iconc.cancel, Styles.flatt, () -> rebuildContentSelect(null)).with(consTextB).growX();
        });
        cfgTable.row();

        tmpIndex2 = 0;
        cfgTable.table(t -> {
            t.defaults().fill();
            t.setBackground(Styles.grayPanel);
            data.configs[block.id].each(c -> {
                var button = t.button("", Styles.flatTogglet, () -> {}).with(consTextB).growX().get();
                button.getLabel().setText(() -> "" + c.amount);
                button.setChecked(true);
                t.button("" + Iconc.copy, Styles.flatt, () -> {
                    data.configs[block.id].add(c.copy());
                    data.update();
                    rebuildContentSelect(block);
                }).with(consTextB).width(48f);
                t.button("" + Iconc.cancel, Styles.flatt, () -> {
                    data.configs[block.id].remove(c);
                    data.update();
                    rebuildContentSelect(block);
                }).with(consTextB).disabled(c.isDefault).width(48f);;
                t.row();
                t.collapser(c::buildBlockConfig, false, button::isChecked).colspan(3);
                t.row();
            });
        });
    }

    public static float cumsum(Floatf<Block> func){
        tmpFloat = 0;
        content.blocks().each(block -> tmpFloat += func.get(block));
        return tmpFloat;
    }
}