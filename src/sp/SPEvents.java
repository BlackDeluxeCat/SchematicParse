package sp;

import mindustry.ctype.*;
import sp.struct.*;

public class SPEvents{
    public static class PreGenerateEvent{}
    public static class GenerateEvent{
        public static final GenerateEvent event = new GenerateEvent();
        public UnlockableContent type;
        public GenerateEvent set(UnlockableContent type){
            this.type = type;
            return this;
        }
    }
    public static class AfterGenerateEvent{}
}
