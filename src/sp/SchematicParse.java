package sp;

import arc.*;
import arc.func.Cons;
import arc.func.Floatf;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
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

    public static Table buttonsTable, selectTable, statsTable;
    public static Cons<TextButton> consTextB = c -> {
        c.getLabel().setAlignment(Align.center);
        c.getLabel().setWrap(false);
        c.margin(6f);
    };

    public SchematicParse(){
        Log.info("SchematicParse Ready For Rush");
        SchematicData.initHandlers();
        selectTable = new Table();
        data = new SchematicData();
        //listen for game load event
        Events.on(ClientLoadEvent.class, e -> {
            data.reset();
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
                            buttonsTable = t;
                            t.defaults().uniform();
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
                            }).height(40f).with(c -> {
                                c.getLabel().setWrap(false);
                                c.getLabelCell().pad(2);
                            });
                        });

                        spt.pane(t -> {
                            statsTable = t;
                            t.setBackground(Styles.grayPanel);
                            t.margin(4f);
                            t.defaults().uniform().left().pad(1f).fill();

                            tmpIndex = 0;
                            for(var item : content.items()){
                                if(!data.getItemDisplay(item.id, -1)) continue;
                                t.button("", Styles.flatt, () -> rebuildContentSelect(item)).with(b -> {
                                    b.getLabel().setText(() -> {
                                        float amt = data.getItem(item.id);
                                        return item.emoji() + (amt > 0f ? "+" : "") + Strings.autoFixed(amt, 2);
                                    });
                                    consTextB.get(b);
                                });
                                if(Mathf.mod(tmpIndex++, 4) == 3) t.row();
                            }

                            for(var item : content.liquids()){
                                if(!data.getLiquidDisplay(item.id, -1)) continue;
                                t.button("", Styles.flatt, () -> rebuildContentSelect(item)).with(b -> {
                                    b.getLabel().setText(() -> {
                                        float amt = data.getLiquid(item.id);
                                        return item.emoji() + (amt > 0f ? "+" : "") + Strings.autoFixed(amt, 2);
                                    });
                                    consTextB.get(b);
                                });
                                if(Mathf.mod(tmpIndex++, 4) == 3) t.row();
                            }

                            t.add(Iconc.blockHeatSource + "+" + Strings.fixed(cumsum(sche, tile -> tile.block instanceof HeatProducer b ? b.heatOutput : 0f), 1));
                            t.row();

                        }).with(p -> {
                            p.setupFadeScrollBars(0.3f, 0.3f);
                            p.setFadeScrollBars(true);
                        });

                        rebuildContentSelect(null);

                        spt.pane(selectTable).with(p -> {
                            p.setupFadeScrollBars(0.3f, 0.3f);
                            p.setFadeScrollBars(true);
                        });
                    }).maxHeight(300f).growX();
                }
            }
        }));
    }

    public static void rebuildContentSelect(@Nullable UnlockableContent ucontent){
        selectTable.clear();

        if(ucontent != null){
            selectTable.table(t -> {
                t.image(ucontent.uiIcon).size(16f);
                t.add(ucontent.localizedName);
                t.button("" + Iconc.cancel, Styles.flatt, () -> rebuildContentSelect(null)).with(consTextB);
            });
            selectTable.row();
        }

        tmpIndex2 = 0;
        selectTable.table(t -> {
            t.defaults().uniform().fill();
            t.setBackground(Styles.grayPanel);
            for(var block : content.blocks()){
                if(ucontent instanceof Item item && data.getItemDisplay(item.id, block.id)){
                    if(Mathf.mod(tmpIndex2++, 5) == 0) t.row();
                    t.button("" + data.blocks[block.id], new TextureRegionDrawable(block.uiIcon), Styles.flatTogglet, 16f, () -> data.putItemSign(item.id, block.id, !data.getItemSign(item.id, block.id))).with(consTextB).with(c -> c.setChecked(data.getItemSign(item.id, block.id)));
                }else if(ucontent instanceof Liquid liquid && data.getLiquidDisplay(liquid.id, block.id)){
                    if(Mathf.mod(tmpIndex2++, 5) == 0) t.row();
                    t.button("" + data.blocks[block.id], new TextureRegionDrawable(block.uiIcon), Styles.flatTogglet, 16f, () -> data.putLiquidSign(liquid.id, block.id, !data.getLiquidSign(liquid.id, block.id))).with(consTextB).with(c -> c.setChecked(data.getItemSign(liquid.id, block.id)));
                }else if(ucontent == null && data.blocks[block.id] > 0){
                    if(Mathf.mod(tmpIndex2++, 5) == 0) t.row();
                    t.button("" + data.blocks[block.id], new TextureRegionDrawable(block.uiIcon), Styles.flatTogglet, 16f, () -> data.putSign(block.id, !data.getSign(block.id))).with(consTextB).with(c -> c.setChecked(data.getSign(block.id)));
                }
            }
        });
    }

    public static float cumsum(Schematic sche, Floatf<Schematic.Stile> func){
        tmpFloat = 0;
        sche.tiles.each(tile -> tmpFloat += func.get(tile));
        return tmpFloat;
    }
}