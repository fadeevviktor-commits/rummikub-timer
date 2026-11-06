package ru.tileclock;

/** Window-relative coordinates in dp; independent of density and system insets. */
public final class GameLayoutPlan {
    public static final class Box {
        public final float left, top, right, bottom;
        Box(float l,float t,float r,float b) { left=l; top=t; right=r; bottom=b; }
    }
    // Header, players, turn, name, timer, hint, next player, controls.
    public static Box[] arrange(float width,float height) {
        Box[] b=new Box[8];
        float pad=width<360?12:20, edge=8, gap=6, right=width-pad;
        if(width>height) {
            float middle=width*.47f;
            b[0]=new Box(pad,edge,right,edge+48);
            float top=edge+48+gap, bottom=height-edge;
            float available=bottom-top;
            float flexible=Math.max(0,available-104-4*gap);
            int[] order={1,2,3,6,7};
            float[] heights={flexible*.5f,flexible*.18f,flexible*.32f,52,52};
            float y=top;
            for(int i=0;i<order.length;i++) {
                b[order[i]]=new Box(pad,y,middle-gap,y+heights[i]); y+=heights[i]+gap;
            }
            b[4]=new Box(middle+gap,top,right,bottom-32-gap);
            b[5]=new Box(middle+gap,bottom-32,right,bottom);
        } else {
            float[] heights={48,64,20,40,0,32,52,52};
            float fixed=2*edge+7*gap;
            for(float h:heights)fixed+=h;
            heights[4]=Math.max(0,height-fixed);
            float y=edge;
            for(int i=0;i<8;i++) { b[i]=new Box(pad,y,right,y+heights[i]); y+=heights[i]+gap; }
        }
        return b;
    }
}
