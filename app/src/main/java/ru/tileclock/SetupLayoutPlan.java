package ru.tileclock;

/** Body coordinates and pinned footer, in usable-window dp. */
public final class SetupLayoutPlan {
    public final GameLayoutPlan.Box[] boxes=new GameLayoutPlan.Box[7];
    public final int columns;
    public final float bodyHeight, viewport;
    public SetupLayoutPlan(float width,float height,int players,boolean keyboard) {
        float pad=width<360?12:20, gap=6, edge=8, right=width-pad;
        viewport=Math.max(0,height-72);
        columns=width>=600 || height<740?2:1;
        boxes[5]=new GameLayoutPlan.Box(pad,height-64,right,height-8);
        if(keyboard) {
            for(int i:new int[]{0,1,3,4,6})boxes[i]=new GameLayoutPlan.Box(pad,0,right,0);
            float editor=((players+columns-1)/columns)*56;
            bodyHeight=Math.max(viewport,editor+16);
            boxes[2]=new GameLayoutPlan.Box(pad,edge,right,edge+editor);
        } else {
            float editor=((players+columns-1)/columns)*56;
            float fixed=2*edge+5*gap+48+editor+64+72+48;
            float header=Math.max(72,Math.min(160,viewport-fixed));
            bodyHeight=Math.max(viewport,header+fixed);
            float[] heights={header,48,editor,72,48,56,64};
            float y=edge;
            for(int i:new int[]{0,1,2,6,3,4}) {
                boxes[i]=new GameLayoutPlan.Box(pad,y,right,y+heights[i]); y+=heights[i]+gap;
            }
        }
    }
}
