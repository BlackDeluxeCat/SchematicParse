package sp;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import sp.struct.*;
import sp.utils.*;

import java.util.*;

public class Calculator extends BaseDialog{
    public static Calculator ui = new Calculator("@ui.calculator.title");
    public Seq<Seq<Entity>> entitiesTab = new Seq<>();
    public Seq<Entity> entities = new Seq<>();
    public ObjectMap<Object, Factor<?>> usedTypes = new ObjectMap<>();

    protected Table entitiesUI, iconsTable, selectUI, statsTable, configUI;
    protected BaseDialog balancingDialog;

    public UnlockableContent currentSelect;

    public Calculator(String s){
        super(s);
        entitiesUI = new Table();
        iconsTable = new Table();
        selectUI = new Table();
        statsTable = new Table();
        configUI = new Table();

        addCloseButton();
        buildEntities();
        buildSelectTable();
        buildStatsTable();
        build();

        balancingDialog = new BaseDialog("@ui.balancing");
        balancingDialog.addCloseButton();

        onResize(this::build);
    }

    public void build(){
        cont.clear();

        cont.table(t -> {
            t.add(entitiesUI).growX();
            t.row();
            t.image().height(2f).growX().color(Color.pink);
            t.row();
            t.add(statsTable).growX().height(ScreenSense.height(0.2f/Scl.scl()));
        }).grow();

        if(ScreenSense.horizontal()){
            cont.image().width(2f).growY().color(Color.pink);
        }else{
            cont.row();
            cont.image().height(2f).growX().color(Color.pink);
            cont.row();
        }

        cont.stack(selectUI, configUI).self(c -> {
            if(ScreenSense.horizontal()){
                c.growY().width(450f);
            }else{
                c.growX().height(300f);
            }
        });

        animeConfig(false);
    }

    public void importShow(ObjectIntMap<UnlockableContent> list){
        entities = new Seq<>();
        entitiesTab.add(entities);
        onUpdate();
        show();
    }

    public void onUpdate(){
        entities.each(e -> e.handler.handle(e));
        buildStatsTable();
        buildEntities();
    }

    public void buildEntities(){
        var et = entitiesUI;
        et.clear();
        et.background(Styles.black8);
        //TODO tabs
        et.table();
        et.row();

        //main
        et.pane(p -> {
            p.defaults().top().pad(20f);
            int i = 0;
            for(var e : entities){
                if(Mathf.mod(i++, 6) == 0) p.row();
                p.table(t -> {
                    t.table(e::buildHead).growX();
                    t.row();
                    t.pane(tt -> {
                        e.info = tt;
                        e.buildInfos(tt);
                    }).growY().minHeight(64f).maxHeight(300f);
                    t.clicked(() -> Time.run(1f, () -> buildConfig(e)));
                });
            }
        }).grow();
    }

    public void buildConfig(Entity e){
        configUI.clear();
        configUI.background(Styles.grayPanel);
        configUI.touchable = Touchable.enabled;

        if(ScreenSense.horizontal()){
            configUI.button(">", () -> animeConfig(false)).growY().left().width(30f).get().getLabel().setFontScale(1f, 2f);
        }else{
            configUI.button("V", () -> animeConfig(false)).growX().left().height(30f).get().getLabel().setFontScale(2f, 1f);
            configUI.row();
        }

        configUI.table(cont -> {
            //title, delete
            cont.table(t -> {
                t.table(e::buildHead).width(150f);
                t.labelWrap(e.type.localizedName).growX().labelAlign(Align.center);
                t.button("" + Iconc.cancel, () -> {
                    entities.remove(e);
                    animeConfig(false);
                    onUpdate();
                }).size(64f);
            }).growX().pad(20f);
            cont.row();

            //cfgs
            cont.table(e::buildConfig).grow().get().clicked(() -> {
                e.updateInfos();
                buildStatsTable();
            });
            cont.row();

            //balancing, close
            cont.table(t -> {
                t.defaults().top().growX();
                t.button("Balancing", () -> {
                    buildFactoryBalancing(e);
                });
            }).growX().pad(20f);
        }).grow();

        //popup anime
        animeConfig(true);
    }

    public void animeConfig(boolean out){
        float bottom = this.buttons.getHeight();
        float w = configUI.getWidth()*1.1f, h = configUI.getHeight()*1.1f + bottom;
        float x = configUI.translation.x, y = configUI.translation.y;
        boolean hor = ScreenSense.horizontal();
        configUI.clearActions();
        configUI.actions(Actions.translateBy(-x + (!hor ? 0f : out ? 0 : w), -y + (hor ? 0f : out ? 0f : -h), 0.3f, Interp.fastSlow));
    }

    public String textFilter = "";
    public Factor<?> factorFilter;
    public void buildIconsTable(){
        iconsTable.clear();
        int i = 0;
        for(var e : Entities.defaults.keys()){
            if(!Objects.equals(textFilter, "") && !e.localizedName.contains(textFilter) && !e.name.contains(textFilter)) continue;
            if(factorFilter != null && !Entities.get(e).factors.contains(f -> f.type.equals(factorFilter.type))) continue;

            if(Mathf.mod(i++, 8) == 0) iconsTable.row();
            iconsTable.button(new TextureRegionDrawable(e.uiIcon), () -> {
                currentSelect = e;
            }).size(48f);
        }
    }

    public void buildSelectTable(){
        var st = selectUI;
        st.clear();

        //search
        st.table(t -> {
            t.field(textFilter, str -> {
                textFilter = str;
                buildIconsTable();
            }).growX().with(tf -> {
                tf.setMessageText("\\__");
                tf.update(() -> {
                    if(textFilter.equals("")) tf.clearText();
                });
            });

            t.button("X", () -> {
                textFilter = "";
                buildIconsTable();
            }).size(32f);

            t.label(() -> factorFilter == null ? "" : factorFilter.type.toString()).size(32f);

            t.button("X", () -> {
                factorFilter = null;
                buildIconsTable();
            }).size(32f);
        }).growX().pad(10f);

        st.row();

        //icon
        buildIconsTable();
        st.pane(iconsTable).grow();

        st.row();

        //info & switch
        st.table(t -> {
            t.button("Add", () -> {
                addNewEntity(currentSelect);
            }).disabled(b -> currentSelect == null).size(64f);
            t.image(() -> currentSelect == null ? Blocks.air.uiIcon : currentSelect.uiIcon).size(64f);
            t.labelWrap(() -> currentSelect == null ? "" : currentSelect.localizedName).growX();
        }).growX().minHeight(100f);
    }

    public void buildStatsTable(){
        var st = statsTable;
        usedTypes.clear();
        entities.each(e -> {
            e.factors.each(f -> !Mathf.zero(f.getRate()), f -> usedTypes.put(f.type, f));
        });

        st.clear();

        st.pane(p -> {
            final int[] i = new int[1];
            usedTypes.each((type, f) -> {
                if(Mathf.mod(i[0]++, 4) == 0) p.row();  //TODO dynamic table
                p.button(b -> {
                    b.table(t -> f.buildIcon(t, false));
                    b.labelWrap(() -> Strings.autoFixed(getRate(type), 4)).width(90f);
                }, () -> {
                    factorFilter = f;
                    buildIconsTable();
                });
            });
        }).grow();
    }


    public void buildFactoryBalancing(Entity entity){
        var dialog = balancingDialog;
        dialog.cont.clear();
        dialog.cont.table(table -> {
            table.table(entity::buildHead).growX();
            table.row();
            table.pane(entity::buildInfos).growY().minHeight(64f).maxHeight(300f);
        });
        dialog.cont.row();
        dialog.cont.pane(p -> {
            for(var type : usedTypes.keys()){
                if(getRate(type) * entity.getRate(type) < 0f){
                    float tweak = entity.balance(type, -getRate(type));
                    p.button(b -> {
                        b.table(t -> {
                            usedTypes.get(type).buildIcon(t, true);
                            t.add("" + getRate(type));
                        }).colspan(2);
                        b.row();

                        b.add("Tweak: " + (tweak>=0?"+":"") + tweak);
                        b.image(entity.type.uiIcon).size(32f);
                    }, () -> {
                        entity.count += tweak;
                        dialog.hide();
                    }).minSize(300f, 100f);
                    p.row();
                }
            }
        }).grow();
        dialog.show();
    }

    public void addNewEntity(UnlockableContent u){
        entities.add(Entities.get(u).copy(false));
        onUpdate();
    }

    public float getRate(Object type){
        return entities.sumf(enitiy -> enitiy.getRate(type) * enitiy.count);
    }
}