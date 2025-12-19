package sp.ui;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mt.utils.*;

import java.util.regex.*;

import static mindustry.Vars.schematics;

public class TagEditorDialog extends BaseDialog{
    public static TagEditorDialog tagEditor = new TagEditorDialog();

    public static final float tagh = 42f;
    public String search = "";
    public TextField searchField;
    public Runnable rebuildPane = () -> {}, rebuildTags = () -> {};
    public final Pattern ignoreSymbols = Pattern.compile("[`~!@#$%^&*()\\-_=+{}|;:'\",<.>/?]");
    public Seq<String> tags, selectedTags = new Seq<>(), handleTags = new Seq<>();
    public boolean modeAdd, modeRemove;
    public TagEditorDialog(){
        super("@schematics");

        shouldPause = true;
        addCloseButton();
        buttons.button("@ui.tagedit.help", () -> {
            Vars.ui.showConfirm("@ui.tagedit.help.content", () -> {});
        });
        shown(this::setup);
        onResize(this::setup);
    }

    public void setup(){
        tags = RefUtils.getValue(Vars.ui.schematics, "tags");
        search = "";

        cont.top();
        cont.clear();

        cont.table(s -> {
            s.left();
            s.image(Icon.zoom);
            searchField = s.field(search, res -> {
                search = res;
                rebuildPane.run();
            }).growX().get();
            searchField.setMessageText("@schematic.search");
            searchField.clicked(KeyCode.mouseRight, () -> {
                if(!search.isEmpty()){
                    search = "";
                    searchField.clearText();
                    rebuildPane.run();
                }
            });
        }).fillX().padBottom(4);

        cont.row();

        cont.table(in -> {
            in.left();
            in.add("@ui.tagedit.listfilter").padRight(4);

            //tags (no scroll pane visible)
            in.pane(Styles.noBarPane, t -> {
                rebuildTags = () -> {
                    t.clearChildren();
                    t.left();

                    t.defaults().pad(2).height(tagh);
                    for(var tag : tags){
                        t.button(tag, Styles.togglet, () -> {
                            if(selectedTags.contains(tag)){
                                selectedTags.remove(tag);
                            }else{
                                selectedTags.add(tag);
                            }
                            rebuildPane.run();
                        }).checked(selectedTags.contains(tag)).with(c -> c.getLabel().setWrap(false));
                    }
                };
                rebuildTags.run();
            }).fillX().height(tagh).scrollY(false);
        }).height(tagh).fillX().padBottom(4f);

        cont.row();

        cont.table(in -> {
            in.left();
            in.add("@ui.tagedit.op").padRight(4);

            in.button(Core.bundle.get("ui.tagedit.op.add") + Iconc.add, Styles.togglet, () -> {
                modeAdd = !modeAdd;
                if(modeAdd) modeRemove = false;
            }).checked(tb -> modeAdd).height(tagh);

            in.button(Core.bundle.get("ui.tagedit.op.remove") + Iconc.cancel, Styles.togglet, () -> {
                modeRemove = !modeRemove;
                if(modeRemove) modeAdd = false;
            }).checked(tb -> modeRemove).height(tagh);

            in.image().width(2f).padLeft(2f).padRight(2f).growY();

            //tags (no scroll pane visible)
            in.pane(Styles.noBarPane, t -> {
                rebuildTags = () -> {
                    t.clearChildren();
                    t.left();

                    t.defaults().pad(2).height(tagh);
                    for(var tag : tags){
                        t.button(tag, Styles.togglet, () -> {
                            if(handleTags.contains(tag)){
                                handleTags.remove(tag);
                            }else{
                                handleTags.add(tag);
                            }
                            rebuildPane.run();
                        }).checked(handleTags.contains(tag)).with(c -> c.getLabel().setWrap(false));
                    }
                };
                rebuildTags.run();
            }).fillX().height(tagh).scrollY(false);
        }).height(tagh).fillX();

        cont.row();

        cont.pane(t -> {
            t.top();

            rebuildPane = () -> {
                int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);

                t.clear();
                int i = 0;
                String searchString = ignoreSymbols.matcher(search.toLowerCase()).replaceAll("");

                Seq<Schematic> schemes = schematics.all().select(s -> (selectedTags.isEmpty() || s.labels.containsAll(selectedTags)) && (search.isEmpty() || ignoreSymbols.matcher(s.name().toLowerCase()).replaceAll("").contains(searchString)));

                for(Schematic s : schemes){
                    t.button(b -> {
                        b.stack(new SchematicsDialog.SchematicImage(s).setScaling(Scaling.fit), new Table(n -> {
                            n.top();
                            n.table(Styles.black3, c -> {
                                Label label = c.add(s.name()).style(Styles.outlineLabel).color(Color.white).top().growX().maxWidth(200f - 8f).get();
                                label.setEllipsis(true);
                                label.setAlignment(Align.center);
                                label.update(() -> label.setColor(s.labels.containsAll(handleTags) ? Pal.accent : Color.gray));
                            }).growX().margin(1).pad(4).maxWidth(Scl.scl(200f - 8f)).padBottom(0);
                        })).size(200f);
                    }, Styles.flatt, () -> {
                        for(String handle : handleTags){
                            if(modeAdd){
                                s.labels.addUnique(handle);
                            }else if(modeRemove){
                                s.labels.remove(handle);
                            }
                        }
                        s.save();
                        rebuildPane.run();
                    });

                    if(++i % cols == 0){
                        t.row();
                    }
                }

                if(schemes.isEmpty()){
                    if(!searchString.isEmpty() || selectedTags.any()){
                        t.add("@none.found");
                    }else{
                        t.add("@none").color(Color.lightGray);
                    }
                }
            };

            rebuildPane.run();
        }).grow().scrollX(false);
    }
}
