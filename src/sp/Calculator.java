package sp;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.dialogs.*;
import sp.struct.*;

import static mindustry.Vars.*;

public class Calculator extends BaseDialog{
    public static Calculator ui = new Calculator("@ui.title");

    public Seq<IOBody> bodies = new Seq<>();
    public ObjectMap<Object, Cons2<Object, Table>> usedTypes = new ObjectMap<>();
    public BaseDialog selectDialog = new BaseDialog("Select");

    public Calculator(String s){
        super(s);
        addCloseButton();
        selectDialog.addCloseButton();
        shown(this::rebuild);

        Events.on(EventType.ContentInitEvent.class, e -> {
            buildSelect();
        });
    }

    public void buildSelect(){
        selectDialog.cont.clear();

        selectDialog.cont.pane(p -> {
            p.defaults().uniform().fill();
            final int[] co = {0};
            IOBody.defaults.each(def -> {
                if(def.factors == null || def.factors.isEmpty()) return;
                p.button(t -> {
                    t.image(def.content.uiIcon).size(iconSmall);
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

    public void showSche(){
        show();
    }

    public void rebuild(){
        cont.clear();
        cont.table(t -> {
            t.name = "Add Table";
            t.button("Add", () -> selectDialog.show());
        });
        cont.row();
        cont.pane(t -> {
            t.name = "Cfg Table";
            final int[] co = {0};
            bodies.each(body -> {
                t.pane(tb -> {
                    tb.image().growX().height(4f).color(Color.coral);
                    tb.row();

                    tb.image(body.content.uiIcon).size(128f).left();
                    tb.row();

                    tb.table(cfgt -> {
                        cfgt.label(() -> Mathf.ceil(body.count) + "[gray](" + Strings.autoFixed(body.count, 2) + ")").get().setAlignment(Align.left);
                        cfgt.button("" + Iconc.cancel, () -> {
                            bodies.remove(body);
                            rebuild();
                        }).size(32f);
                        cfgt.row();
                        cfgt.field(String.valueOf(body.count), TextField.TextFieldFilter.floatsOnly, s -> body.count = Strings.parseFloat(s)).colspan(2).update(tf -> tf.setText(String.valueOf(body.count)));
                    });
                    tb.row();

                    body.factors.each(fac -> {
                        tb.table(fac::build);
                        tb.row();
                        usedTypes.put(fac.type, (type, uit) -> fac.buildIcon(uit));
                    });

                }).top().maxHeight(500f);
                if(Mathf.mod(++co[0], 6) == 0) t.row();
            });
        }).growY();
        cont.row();
        cont.table(t -> {
            t.name = "Stat Table";
            final int[] co = {0};
            usedTypes.each((b, cons) -> {
                t.table(it -> cons.get(b, it));
                t.label(() -> {
                    float f = getFactor(b);
                    return (f >= 0f ? "+":"") + Strings.autoFixed(f, 2);
                });
                if(Mathf.mod(++co[0], 4) == 0) t.row();
            });
        });
    }

    public float getFactor(Object type){
        return bodies.sumf(body -> body.count * body.getRate(type) * 60f);
    }
}
