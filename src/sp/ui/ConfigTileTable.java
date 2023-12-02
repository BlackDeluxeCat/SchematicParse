package sp.ui;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.world.*;

public class ConfigTileTable extends Table{
    public int tileWidth = 1, tileHeight = 1;

    public Block[] items;

    public ConfigTileTable(int w, int h){
        items = new Block[w*h];
        tilesSize(w, h);
    }

    public void tilesSize(int w, int h){
        tileWidth = w;
        tileHeight = h;
        Block[] items = new Block[size()];
        System.arraycopy(this.items, 0, items, 0, Math.min(this.items.length, size()));
        this.items = items;
    }

    public boolean pour(Block item, int x, int y){
        if(x<0 || x>=tileWidth || y<0 || y>=tileHeight) return false;
        items[x+tileWidth*y] = item;
        return true;
    }

    public int x(int index){
        return index % tileWidth;
    }

    public int y(int index){
        return index / tileWidth;
    }

    public @Nullable Block pick(int x, int y){
        if(x<0 || x>=tileWidth || y<0 || y>=tileHeight) return null;
        return items[x+tileWidth*y];
    }

    public int size(){
        return tileWidth*tileHeight;
    }

    public class TileImage extends Element{
        public static float tw = Blocks.air.uiIcon.width, half = tw / 2f;
        public Cons<Object> drawer;
        @Override
        public void draw(){
            super.draw();
            if(drawer == null) return;
            for(int i = 0; i < items.length; i++){
                if(items[i] == null) return;
                Draw.rect(items[i].uiIcon, x(i) * tw + half, y(i) * tw + half);
            }
        }

        @Override
        public float getPrefWidth(){
            return Blocks.air.uiIcon.width * tileWidth;
        }

        @Override
        public float getPrefHeight(){
            return Blocks.air.uiIcon.width * tileHeight;
        }
    }
}
