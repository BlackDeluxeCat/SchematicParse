package sp.struct;

public interface Simulator{
    void get(Entity e);

    Simulator none = e -> {};

    Simulator overdrive = e -> {
        var od = e.factors.find(f -> f.type.equals("overdrive"));
        if(od == null) return;
        float odr = od.enable ? od.rate : 1f;
        e.factors.each(f -> !f.type.equals("overdrive"), f -> f.efficiency *= odr);
    };
}