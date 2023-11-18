package sp;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import sp.struct.*;
import sp.ui.*;

import static sp.SchematicParse.*;

public class Calculator extends BaseDialog{
    public static Calculator ui = new Calculator("@ui.calculator.title");

    public Seq<Seq<Entity>> entitiesTab = new Seq<>();
    public Seq<Entity> entities = new Seq<>();
    public ObjectSet<Object> usedTypes = new ObjectSet<>();
    public BaseDialog selectDialog = new BaseDialog("@ui.selectfactory.title"), filterSelectDialog = new BaseDialog("@ui.selectfactory.title"), balancingDialog = new BaseDialog("@ui.balancing.title");
    protected Table entitiesPane;
    protected boolean needRebuildStats = true;

    public Calculator(String s){
        super(s);
        addCloseButton();

    }



    public void importShow(ObjectIntMap<UnlockableContent> list){
        entities = new Seq<>();
        entitiesTab.add(entities);

        show();
    }

    public void build(){
        usedTypes.clear();

        cont.clear();
        cont.table(t -> {
            t.name = "Tab Table";
            t.background(Styles.grayPanel);
            t.margin(4f, 4f, 0f, 0f);

            t.pane(p -> {
                rebuildTabs(p);
            }).with(p -> {
                p.setForceScroll(true, false);
                p.setScrollingDisabled(false, true);
                p.setScrollBarPositions(false, false);
                p.setFadeScrollBars(false);
                p.setOverscroll(false, false);
            }).growX();

            t.table(toolt -> {
                toolt.defaults().size(50f);
                toolt.button(Iconc.copy + "", Styles.flatt, () -> {
                    var seq = new Seq<Entity>();
                    entities.each(e -> seq.add(e.copy()));
                    entities = seq;
                    entitiesTab.add(seq);

                    build();
                });

                toolt.button(Iconc.add + "", Styles.flatt, () -> {
                    var seq = new Seq<Entity>();
                    entitiesTab.add(seq);
                    entities = seq;

                    build();
                });
            });
        }).height(50f).growX();

        cont.row();

        cont.table(t -> {
            t.name = "Add Table";
            t.defaults().height(100f);
            t.button("@ui.addfactory", () -> selectDialog.show()).growX();
            t.button(Iconc.blockItemSource + "\n" + Core.bundle.get("ui.addsource") , () -> {
                entities.add(SourceEntity.source.copy());
                rebuildEntities(entitiesPane);
            }).size(100f);
        }).growX();

        cont.row();

        cont.pane(t -> {
            entitiesPane = t;
            rebuildEntities(t);
        }).growY();

        cont.row();

        cont.pane(t -> {
            t.name = "Stat Table";
            t.defaults().pad(4f).fill();

            t.update(() -> {
                if(!needRebuildStats) return;
                needRebuildStats = false;
                t.clear();
                final int[] co = {0};

                usedTypes.each(type -> {
                    var fac = Factor.factors.get(type);
                    t.button(tt -> {
                        tt.table(it -> fac.buildIcon(it, false));
                        tt.add("").update(l -> {
                            float f = getFactor(type);
                            l.setText((f >= 0f ? "+":"") + Strings.autoFixed(f, 3));
                            l.setColor(Mathf.zero(f, 0.01f) ? Color.gray : f > 0 ? Color.green : Color.coral);
                        });
                    }, Styles.flatBordert, () -> {
                        filterSelectDialog.cont.clear();
                        filterSelectDialog.cont.table(taaa -> {
                            fac.buildIcon(taaa, true);
                            taaa.add(Strings.fixed(getFactor(type), 3));
                        }).row();
                        filterSelectDialog.cont.pane(p -> {
                            p.defaults().uniform().fill().pad(2f);
                            final int[] co2 = {0};
                            float total = getFactor(type);
                            BlockEnitiy.defaults.each(def -> {
                                if(def.factors == null || def.factors.isEmpty()) return;
                                if(!def.factors.contains(factor -> factor.type.equals(type) && factor.rate * total < 0f)) return;
                                var targetfac = def.factors.min(fff -> fff.type.equals(type) ? fff.rate * Mathf.sign(total) : 0f);
                                p.button(ttt -> {
                                    ttt.image(def.type.uiIcon).size(32f);
                                    ttt.add(def.type.localizedName).growX();
                                    ttt.add(Strings.fixed(targetfac.rate, 3));
                                }, Styles.flatBordert, () -> {
                                    entities.add(def.copy());
                                    rebuildEntities(entitiesPane);
                                    filterSelectDialog.hide();
                                });
                                if(Mathf.mod(++co2[0], 3) == 0) p.row();
                            });
                        }).grow();
                        filterSelectDialog.show();
                    });

                    if(Mathf.mod(++co[0], 6) == 0) t.row();
                });
            });
        }).maxHeight(300f);
    }

    public float getFactor(Object type){
        return getFactor(entities, type);
    }

    public float getFactor(Seq<Entity> entites, Object type){
        return entites.sumf(enitiy -> enitiy.getRate(type));
    }
}
