package sp;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
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

import static sp.SchematicParse.*;

public class Calculator extends BaseDialog{
    public static Calculator ui = new Calculator("@ui.calculator.title");

    public Seq<IOEnitiy> bodies = new Seq<>();
    public ObjectMap<Object, FactorIO<?>> usedTypes = new ObjectMap<>();
    public BaseDialog selectDialog = new BaseDialog("@ui.selectfactory.title"), trimDialog = new BaseDialog("@ui.trim.title");

    public Calculator(String s){
        super(s);
        addCloseButton();
        selectDialog.addCloseButton();
        trimDialog.addCloseButton();
        shown(this::rebuild);

        onResize(this::rebuild);

        Events.on(EventType.ContentInitEvent.class, e -> {
            buildSelect();
        });
    }

    public void buildSelect(){
        selectDialog.cont.clear();

        selectDialog.cont.pane(p -> {
            p.defaults().uniform().fill();
            final int[] co = {0};
            IOEnitiy.defaults.each(def -> {
                if(def.factors == null || def.factors.isEmpty()) return;
                p.button(t -> {
                    t.image(def.content.uiIcon).size(32f);
                    t.add(def.content.localizedName).growX();
                }, () -> {
                    bodies.add(def.copy());
                    rebuild();
                    selectDialog.hide();
                });
                if(Mathf.mod(++co[0], 6) == 0) p.row();
            });
        }).grow();
    }

    public void importShow(ObjectIntMap<UnlockableContent> list){
        bodies.clear();
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

    public void rebuild(){
        //bodies.sort(enitiy -> -enitiy.factors.size);

        usedTypes.clear();

        cont.clear();

        cont.table(t -> {
            t.name = "Add Table";
            t.defaults().height(100f);
            t.button("@ui.addfactory", () -> selectDialog.show()).growX();
            t.button(Iconc.blockItemSource + "\n" + Core.bundle.get("ui.addsource") , () -> {
                bodies.add(IOEnitiy.SourceIOEntity.source.copy());
                rebuild();
            }).size(100f);
        }).growX();

        cont.row();

        cont.pane(t -> {
            t.name = "Cfg Table";
            final int[] co = {0};
            bodies.each(e -> {
                t.pane(tb -> {
                    tb.image().growX().height(4f).color(Color.gold);
                    tb.row();

                    tb.table(icont -> {
                        var img = new Table(tc -> {
                            tc.image(e.content.uiIcon).size(92f);
                            tc.add().growX();
                        });
                        img.setFillParent(true);
                        var lcc = new Label(() -> "" + Mathf.ceil(e.count));
                        lcc.setFontScale(1.8f);
                        //lcc.setColor(1f, 1f, 1f, 1f);
                        lcc.setFillParent(true);
                        lcc.setAlignment(Align.bottomLeft);
                        var lc = new Label(() -> Strings.fixed(e.count, 3));
                        lc.setFillParent(true);
                        lcc.setStyle(Styles.outlineLabel);
                        lc.setAlignment(Align.bottomLeft);
                        lc.setColor(0.9f, 0.8f, 1f, 0.6f);
                        var buttons = new Table(tc -> {
                            tc.defaults().right();

                            tc.add().growX();
                            tc.button("" + Iconc.cancel, Styles.cleart, () -> {
                                bodies.remove(e);
                                rebuild();
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
                            tc.field(String.valueOf(e.count), s -> e.count = Strings.parseFloat(s, 0f)).maxWidth(80f).height(32f).with(f -> f.setAlignment(Align.right));
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
        }).growY();

        cont.row();

        cont.pane(t -> {
            t.name = "Stat Table";
            final int[] co = {0};
            usedTypes.each((b, fac) -> {
                t.defaults().pad(4f);
                t.table(it -> fac.buildIcon(it, false));
                t.add("").update(l -> {
                    float f = getFactor(b);
                    l.setText((f >= 0f ? "+":"") + Strings.autoFixed(f, 3));
                    l.setColor(Mathf.zero(f, 0.01f) ? Color.gray : f > 0 ? Color.green : Color.coral);
                });

                if(Mathf.mod(++co[0], 6) == 0) t.row();
            });
        }).maxHeight(300f);
    }

    public float getFactor(Object type){
        return bodies.sumf(enitiy -> enitiy.count * enitiy.getRate(type));
    }
}
