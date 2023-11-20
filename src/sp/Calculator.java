package sp;

import arc.math.*;
import arc.scene.actions.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.ui.dialogs.*;
import sp.struct.*;
import sp.utils.*;

public class Calculator extends BaseDialog{
    public static Calculator ui = new Calculator("@ui.calculator.title");
    public Seq<Seq<Entity>> entitiesTab = new Seq<>();
    public Seq<Entity> entities = new Seq<>();
    public ObjectSet<Object> usedTypes = new ObjectSet<>();
    public BaseDialog selectDialog = new BaseDialog("@ui.selectfactory.title"), filterSelectDialog = new BaseDialog("@ui.selectfactory.title"), balancingDialog = new BaseDialog("@ui.balancing.title");
    protected Table entitiesUI, selectUI, selectTable, statsTable, configUI;

    public UnlockableContent currentSelect;
    public boolean showStats;
    protected boolean needRebuildStats = true;

    public Calculator(String s){
        super(s);
        addCloseButton();

        cont.add(entitiesUI).grow();
        cont.stack(selectUI, configUI).self(c -> {
            if(ScreenSense.horizontal()){
                c.growY().width(400f);
            }else{
                c.growX().height(400f);
            }
        });
    }

    public void importShow(ObjectIntMap<UnlockableContent> list){
        entities = new Seq<>();
        entitiesTab.add(entities);

        show();
    }

    public void buildEntities(){
        var et = entitiesUI;
        //tabs
        et.table();
        et.row();

        //main
        et.pane(p -> {
            int i = 0;
            for(var e : entities){
                e.build(p);
                if(i++ > 4) row();
            }
        }).grow();
    }

    public void buildSelect(){
        var st = selectUI;
        //search

        //icon
        if(showStats){
            buildStatsTable();
            st.pane(statsTable).grow();
        }else{
            st.pane(selectTable).grow();
        }

        st.row();

        //info & switch
        st.table(t -> {
            if(!showStats){
                st.button("Add", () -> {
                    addNewEntity(currentSelect);
                }).disabled(b -> currentSelect == null).size(32f);
                st.image(() -> currentSelect == null ? Blocks.air.uiIcon : currentSelect.uiIcon).size(32f);
                st.label(() -> currentSelect == null ? "" : currentSelect.description).growX();
            }
            st.button("Switch", () -> {
                showStats = !showStats;
                buildSelect();
            }).right();
        });
    }

    public void buildConfig(Entity e){
        configUI.clear();

        //title, delete
        configUI.table(t -> {
            e.buildHead(t);
            t.add(e.type.localizedName);
        });
        configUI.row();

        //cfgs
        configUI.table(t -> e.buildConfig(configUI)).grow();

        //balancing, close


        configUI.actions(Actions.translateBy(configUI.translation.x, configUI.translation.y), Actions.translateBy(-configUI.translation.x, -configUI.translation.y, 1f, Interp.fade));
    }

    public void closeConfig(){
        configUI.actions(Actions.translateBy(configUI.translation.x, configUI.translation.y, 1f, Interp.fade));
    }

    public void buildSelectTable(){
        selectTable.clear();
        int i = 0;
        for(var e : Entities.defaults){
            selectTable.button(new TextureRegionDrawable(e.key.uiIcon), () -> {
                currentSelect = e.key;
            }).size(32f);
            if(i++ > 10) selectTable.row();
        }
    }

    public void buildStatsTable(){

    }

    public void addNewEntity(UnlockableContent u){
        entities.add(Entities.get(u).copy());
    }

    public float getFactor(Object type){
        return getFactor(entities, type);
    }

    public float getFactor(Seq<Entity> entites, Object type){
        return entites.sumf(enitiy -> enitiy.getRate(type));
    }
}
