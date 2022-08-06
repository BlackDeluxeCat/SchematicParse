package sp;

import arc.*;
import arc.math.geom.Vec2;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Scl;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.game.Schematic;
import mindustry.gen.Iconc;
import mindustry.mod.*;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.MessageBlock;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import static mindustry.Vars.*;

public class SchematicParse extends Mod{

    public SchematicParse(){
        Log.info("SchematicParse Ready For Rush");

        //listen for game load event
        Events.on(ClientLoadEvent.class, e -> {
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
                info.cont.row();
                String schename = l.getText().toString().replace("[[" + Core.bundle.get("schematic") + "] ", "");
                Schematic sche = schematics.all().find(s -> s.name().equals(schename));
                if(sche != null && sche.width <= maxSchematicSize && sche.height <= maxSchematicSize){
                    info.cont.button("" + Iconc.zoom + Iconc.blockMicroProcessor + Iconc.blockMessage, Styles.flatt, () -> {}).with(b -> {
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
                                        /*
                                        links.clear();

                                        int total = stream.readInt();

                                        if(version == 0){
                                            //old version just had links, ignore those

                                            for(int i = 0; i < total; i++){
                                                stream.readInt();
                                            }
                                        }else{
                                            for(int i = 0; i < total; i++){
                                                String name = stream.readUTF();
                                                short x = stream.readShort(), y = stream.readShort();

                                                if(relative){
                                                    x += tileX();
                                                    y += tileY();
                                                }

                                                Building build = world.build(x, y);

                                                if(build != null){
                                                    String bestName = getLinkName(build.block);
                                                    if(!name.startsWith(bestName)){
                                                        name = findLinkName(build.block);
                                                    }
                                                }

                                                links.add(new LogicLink(x, y, name, false));
                                            }
                                        }
                                        */
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
                }
            }
        }));
    }
}