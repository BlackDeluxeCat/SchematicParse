package sp;

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

public class Calculator extends BaseDialog{
    public static Calculator ui = new Calculator("@ui.calculator.title");
    public Seq<Seq<Entity>> entitiesTab = new Seq<>();
    public Seq<Entity> entities = new Seq<>();
    public ObjectMap<Object, Factor<?>> usedTypes = new ObjectMap<>();

    protected Table entitiesUI, iconsUI, selectTable, statsTable, configUI;
    protected BaseDialog balancingDialog;

    public UnlockableContent currentSelect;
    public boolean showStats;

    public Calculator(String s){
        super(s);
        entitiesUI = new Table();
        iconsUI = new Table();
        selectTable = new Table();
        statsTable = new Table();
        configUI = new Table();

        addCloseButton();
        buildEntities();
        buildSelectTable();
        buildStatsTable();
        buildSelect();
        build();

        balancingDialog = new BaseDialog("@ui.balancing");
        balancingDialog.addCloseButton();

        onResize(this::build);
    }

    public void build(){
        cont.clear();

        cont.add(entitiesUI).grow();

        if(ScreenSense.horizontal()){
            cont.image().width(2f).growY().color(Color.pink);
        }else{
            cont.row();
            cont.image().height(2f).growX().color(Color.pink);
            cont.row();
        }

        cont.stack(iconsUI, configUI).self(c -> {
            if(ScreenSense.horizontal()){
                c.growY().width(450f);
            }else{
                c.growX().height(450f);
            }
        });
    }

    public void importShow(ObjectIntMap<UnlockableContent> list){
        entities = new Seq<>();
        entitiesTab.add(entities);
        onUpdate();
        show();
    }

    public void onUpdate(){
        entities.each(e -> e.handler.handle());
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
                    t.pane(e::buildInfos).growY().minHeight(64f).maxHeight(300f);
                    t.clicked(() -> buildConfig(e));
                });
            }
        }).grow();
    }

    public void buildSelect(){
        var st = iconsUI;
        st.clear();
        st.stack(statsTable, selectTable).grow();
    }

    public void buildConfig(Entity e){
        configUI.clear();
        configUI.background(Styles.grayPanel);
        configUI.touchable = Touchable.enabled;
        configUI.button(">>", this::closeConfig).growY().left().width(50f);

        configUI.table(cont -> {
            //title, delete
            cont.table(t -> {
                t.table(e::buildHead).width(150f);
                t.labelWrap(e.type.localizedName).growX().labelAlign(Align.center);
                t.button("" + Iconc.cancel, () -> {
                    entities.remove(e);
                    closeConfig();
                    onUpdate();
                }).size(64f);
            }).growX().pad(20f).height(300f);
            cont.row();

            //cfgs
            cont.table(e::buildConfig).grow();
            cont.row();

            //balancing, close
            cont.table(t -> {
                t.defaults().top();
                t.button("Balancing", () -> {
                    buildFactoryBalancing(e);
                }).grow();
            }).growX().pad(20f).height(100f);
        }).grow();

        //popup anime
        configUI.actions(Actions.translateTo(configUI.getWidth(), 0f), Actions.translateBy(-configUI.getWidth(), 0f, 0.2f, Interp.fastSlow));
    }

    public void closeConfig(){
        configUI.actions(Actions.translateBy(configUI.getWidth(), 0f, 0.2f, Interp.fastSlow));
    }

    public void buildSelectTable(){
        var st = selectTable;
        st.clear();
        st.visible(() -> !showStats);

        //search
        //TODO

        //icon
        st.pane(t -> {
            int i = 0;
            for(var e : Entities.defaults.keys()){
                if(Mathf.mod(i++, 8) == 0) t.row();
                t.button(new TextureRegionDrawable(e.uiIcon), () -> {
                    currentSelect = e;
                }).size(48f);
            }
        }).grow();

        st.row();

        //info & switch
        st.table(t -> {
            t.button("Add", () -> {
                addNewEntity(currentSelect);
            }).disabled(b -> currentSelect == null).size(64f);
            t.image(() -> currentSelect == null ? Blocks.air.uiIcon : currentSelect.uiIcon).size(64f);
            t.labelWrap(() -> currentSelect == null ? "" : currentSelect.localizedName).growX();
            t.button("Switch", () -> {
                showStats = !showStats;
            }).right();
        }).growX().minHeight(200f);
    }

    public void buildStatsTable(){
        var st = statsTable;
        usedTypes.clear();
        entities.each(e -> {
            e.factors.each(f -> usedTypes.put(f.type, f));
        });

        st.clear();
        st.visible(() -> showStats);

        st.pane(p -> {
            final int[] i = new int[1];
            usedTypes.each((type, f) -> {
                if(Mathf.mod(i[0]++, 3) == 0) p.row();
                p.button(b -> {
                    b.table(t -> f.buildIcon(t, false));
                    b.labelWrap(() -> Strings.autoFixed(getRate(type), 4)).width(90f);
                }, () -> {});
            });
        });

        st.row();

        //info & switch
        st.table(t -> {
            t.button("Switch", () -> showStats = !showStats).right();
        }).growX();
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
        entities.add(Entities.get(u).copy());
        onUpdate();
    }

    public float getRate(Object type){
        return entities.sumf(enitiy -> enitiy.getRate(type) * enitiy.count);
    }
}
