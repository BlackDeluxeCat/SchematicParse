package sp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
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

    public Seq<Seq<IOEnitiy>> bodiesTab = new Seq<>();
    public Seq<IOEnitiy> bodies = new Seq<>();
    public ObjectMap<Object, FactorIO<?>> usedTypes = new ObjectMap<>();
    public BaseDialog selectDialog = new BaseDialog("@ui.selectfactory.title"), filterSelectDialog = new BaseDialog("@ui.selectfactory.title"), trimDialog = new BaseDialog("@ui.trim.title");
    protected Table entitiesPane;
    protected boolean needRebuildStats = true;

    public Calculator(String s){
        super(s);
        addCloseButton();
        selectDialog.addCloseButton();
        filterSelectDialog.addCloseButton();
        trimDialog.addCloseButton();

        shown(() -> {
            if(bodiesTab.size == 0){
                var seq = new Seq<IOEnitiy>();
                bodiesTab.add(seq);
                bodies = seq;
            }
            this.build();
        });

        onResize(this::build);

        Events.on(EventType.ContentInitEvent.class, e -> {
            buildSelect();
        });
    }

    public void buildSelect(){
        selectDialog.cont.clear();

        selectDialog.cont.pane(p -> {
            p.defaults().fill().pad(2f);
            final int[] co = {0};
            IOEnitiy.defaults.each(def -> {
                if(def.factors == null || def.factors.isEmpty()) return;
                p.button(t -> {
                    t.image(def.content.uiIcon).size(32f);
                    t.add(new SPLabel(def.content.localizedName, true, true)).size(128f, 24f);
                }, Styles.flatBordert, () -> {
                    bodies.add(def.copy());
                    rebuildEntities(entitiesPane);
                    selectDialog.hide();
                });
                if(Mathf.mod(++co[0], 6) == 0) p.row();
            });
        }).grow();
    }

    public void importShow(ObjectIntMap<UnlockableContent> list){
        bodies = new Seq<>();
        bodiesTab.add(bodies);
        IOEnitiy.defaults.each(def -> {
            if(def.factors == null || def.factors.isEmpty()) return;
            int count = list.get(def.content, 0);
            if(count <= 0) return;
            var copy = def.copy();
            copy.count = count;
            bodies.add(copy);
        });
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
                    var seq = new Seq<IOEnitiy>();
                    bodies.each(e -> seq.add(e.copy()));
                    bodies = seq;
                    bodiesTab.add(seq);

                    build();
                });

                toolt.button(Iconc.add + "", Styles.flatt, () -> {
                    var seq = new Seq<IOEnitiy>();
                    bodiesTab.add(seq);
                    bodies = seq;

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
                bodies.add(IOEnitiy.SourceIOEntity.source.copy());
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

                usedTypes.each((b, fac) -> {
                    t.button(tt -> {
                        tt.table(it -> fac.buildIcon(it, false));
                        tt.add("").update(l -> {
                            float f = getFactor(b);
                            l.setText((f >= 0f ? "+":"") + Strings.autoFixed(f, 3));
                            l.setColor(Mathf.zero(f, 0.01f) ? Color.gray : f > 0 ? Color.green : Color.coral);
                        });
                    }, Styles.flatBordert, () -> {
                        filterSelectDialog.cont.clear();
                        filterSelectDialog.cont.table(taaa -> {
                            fac.buildIcon(taaa, true);
                            taaa.add(Strings.fixed(getFactor(b), 3));
                        }).row();
                        filterSelectDialog.cont.pane(p -> {
                            p.defaults().uniform().fill().pad(2f);
                            final int[] co2 = {0};
                            float total = getFactor(b);
                            IOEnitiy.defaults.each(def -> {
                                if(def.factors == null || def.factors.isEmpty()) return;
                                if(!def.factors.contains(factor -> factor.type.equals(b) && factor.rate * total < 0f)) return;
                                var targetfac = def.factors.min(fff -> fff.type.equals(b) ? fff.rate * Mathf.sign(total) : 0f);
                                p.button(ttt -> {
                                    ttt.image(def.content.uiIcon).size(32f);
                                    ttt.add(def.content.localizedName).growX();
                                    ttt.add(Strings.fixed(targetfac.rate, 3));
                                }, Styles.flatBordert, () -> {
                                    bodies.add(def.copy());
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

    public void rebuildTabs(Table p){
        p.clear();
        p.left();
        bodiesTab.each(bo -> {
            p.table(tt -> {
                tt.left();
                //Title
                tt.button(bb -> {
                    bb.margin(4f).marginBottom(0f);
                    for(int i = 0; i<7; i++){
                        int finalI = i;
                        bb.add(new Image(){
                            UnlockableContent co;
                            @Override
                            public void act(float delta){
                                if(finalI < bo.size && bo.get(finalI).content != co){
                                    co = bo.get(finalI).content;
                                    this.setDrawable(co.uiIcon);
                                }else if(finalI >= bo.size){
                                    co = null;
                                    this.setDrawable(Core.atlas.has("whiteui") ? Core.atlas.find("whiteui") : Core.atlas.find("white"));
                                    this.setSize(0f);
                                }
                                super.act(delta);
                            }
                        }).maxSize(32f);
                    }

                    var l = new SPLabel("", true, false);
                    l.setText(() -> "" + (int)getFactor(bo, "size"));
                    l.setAlignment(Align.center);
                    bb.add(l).size(40f, 32f);
                }, Styles.clearTogglei, () -> {
                    bodies = bo;
                    rebuildEntities(entitiesPane);
                }).grow().checked(bm -> bodies == bo);

                //Close button
                tt.button("" + Iconc.cancel, Styles.nonet, () -> {
                    int index = bodiesTab.indexOf(bo) - 1;
                    bodiesTab.remove(bo);
                    if(bodies == bo){
                        bodies = bodiesTab.get(Math.max(0, index));
                    }
                    rebuildEntities(entitiesPane);
                    float w = tt.getWidth();
                    tt.setTransform(true);
                    tt.actions(Actions.scaleTo(0f, 1f, 0.2f, a -> {
                        float r = Mathf.pow(a, 3f);
                        p.getCell(tt).width(w * ((1f-r) < 0.05f ? 0f : (1f-r)));
                        return r;
                    }), Actions.run(() -> {
                        p.getCells().remove(p.getCell(tt));
                        tt.remove();
                    }));
                }).size(32f).visible(() -> bodiesTab.size > 1);

            }).checked(bm -> bodies == bo);
        });
    }

    public void rebuildEntities(Table t){
        needRebuildStats = true;
        t.clear();
        t.name = "Cfg Table";
        final int[] co = {0};
        bodies.each(e -> {
            t.pane(tb -> {
                tb.image().growX().height(4f).color(Color.gold);
                tb.row();

                tb.table(icont -> {
                    var img = new Table(tc -> {
                        tc.image(e.content.uiIcon).size(92f).with(c -> c.update(() -> c.setDrawable(e.content.uiIcon)));
                        tc.add().growX();
                    });
                    img.setFillParent(true);
                    var lcc = new Label(() -> "" + Mathf.ceil(e.count - Mathf.FLOAT_ROUNDING_ERROR));
                    lcc.setFontScale(1.8f);
                    //lcc.setColor(1f, 1f, 1f, 1f);
                    lcc.setFillParent(true);
                    lcc.setAlignment(Align.bottomLeft);
                    var lc = new Label(() -> Strings.fixed(e.count, 3));
                    lc.setFillParent(true);
                    lcc.setStyle(Styles.outlineLabel);
                    lc.setAlignment(Align.topLeft);
                    lc.setColor(0.9f, 0.8f, 1f, 0.6f);
                    var buttons = new Table(tc -> {
                        tc.defaults().right();

                        tc.add().growX();
                        tc.button("" + Iconc.cancel, Styles.cleart, () -> {
                            bodies.remove(e);
                            rebuildEntities(t);
                        }).size(32f);
                        tc.row();

                        tc.add().growX();
                        tc.button(uiTrim, Styles.cleari, 24f, () -> {
                            trimDialog.cont.clear();
                            trimDialog.cont.pane(p -> {
                                final int[] co2 = {0};
                                usedTypes.each((type, fac) -> {
                                    float count = e.count;
                                    e.count = 0f;
                                    float need = e.need(type, -getFactor(type));
                                    e.count = count;
                                    if(need <= 0f) return;
                                    p.button(trimt -> fac.buildIcon(trimt, true), () -> {
                                        e.count = need;
                                        trimDialog.hide();
                                    }).minSize(48f).pad(4f);
                                    if(Mathf.mod(++co2[0], 6) == 0) p.row();
                                });
                            });
                            trimDialog.show();
                        }).size(32f);
                        tc.row();

                        tc.add().growX();
                        tc.field(String.valueOf(e.count), s -> e.count = Strings.parseFloat(s, 0f)).maxWidth(80f).height(32f).with(f -> {
                            f.setAlignment(Align.right);
                            f.update(() -> {
                                if(!f.hasKeyboard()) f.setText(String.valueOf(e.count));
                            });
                        });
                    });
                    buttons.setFillParent(true);

                    icont.stack(img, lcc, lc, buttons).width(128f).height(92f);
                });

                tb.row();

                tb.table(e::buildFactors);

                e.factors.each(fac -> usedTypes.put(fac.type, fac));
                e.buckets.each(bucket -> bucket.factors.each(fac -> usedTypes.put(fac.type, fac)));

            }).top().maxHeight(300f).pad(4f);

            if(Mathf.mod(++co[0], Math.max((int)(Core.graphics.getWidth() / Scl.scl(160)), 1)) == 0) t.row();
        });
    }

    public float getFactor(Object type){
        return getFactor(bodies, type);
    }

    public float getFactor(Seq<IOEnitiy> entites, Object type){
        return entites.sumf(enitiy -> enitiy.getRate(type));
    }
}
