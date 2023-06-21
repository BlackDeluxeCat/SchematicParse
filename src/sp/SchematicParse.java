package sp;

import arc.*;
import arc.func.Cons;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.logic.*;
import sp.struct.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import static mindustry.Vars.*;

public class SchematicParse extends Mod{
    public static SchematicData data = new SchematicData();

    public static TextField.TextFieldFilter floatf = (field, c) -> TextField.TextFieldFilter.floatsOnly.acceptChar(field, c) || ((c == '-' && field.getCursorPosition() == 0 && !field.getText().contains("-")));

    public static Cons<TextButton> consTextB = c -> {
        c.getLabel().setAlignment(Align.center);
        c.getLabel().setWrap(false);
        c.margin(6f);
    };

    public SchematicParse(){
        Events.on(ClientLoadEvent.class, e -> {
            initStyles();
            schelogic();
            Time.run(60f, () -> {
                IOEnitiy.init();
                Calculator.ui.buildSelect();
            });
        });
    }

    @Override
    public void loadContent(){
        Log.info("Loading some example content.");
    }

    public static TextureRegionDrawable uiTrim;
    public static void initStyles(){
        uiTrim = new TextureRegionDrawable(Core.atlas.find("schematicparse-ui-balance"));
    }

    public static void schelogic(){
        ui.schematics.buttons.button("@ui.calculator", () -> Calculator.ui.show());

        SchematicsDialog.SchematicInfoDialog info = Reflect.get(SchematicsDialog.class, ui.schematics, "info");

        info.shown(Time.runTask(10f, () -> {
            Label l = info.find(e -> e instanceof Label ll && ll.getText().toString().contains("[[" + Core.bundle.get("schematic") + "] "));
            if(l != null){
                String schename = l.getText().toString().replace("[[" + Core.bundle.get("schematic") + "] ", "");
                Schematic sche = schematics.all().find(s -> s.name().equals(schename));
                if(sche != null && sche.width <= maxSchematicSize && sche.height <= maxSchematicSize){
                    data.read(sche);

                    info.buttons.row();

                    info.buttons.button("" + Iconc.zoom + Iconc.blockMicroProcessor + Iconc.blockMessage, () -> {}).with(b -> {
                        b.getLabel().setWrap(false);
                        b.setDisabled(() -> data.logicparsed);
                        b.clicked(() -> {
                            if(data.logicparsed) return;
                            data.logicparsed = true;
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
                    });

                    info.buttons.button("@ui.calculator", () -> {
                        if(data.calculatorimported){
                            ui.showInfoFade("This schematic has been imported recently, check out the tabs.", 10f);
                            Calculator.ui.show();
                        }else{
                            Calculator.ui.importShow(data.used);
                            data.calculatorimported = true;
                        }
                    });
                }
            }
        }));
    }
}