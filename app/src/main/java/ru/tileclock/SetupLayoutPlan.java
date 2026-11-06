package ru.tileclock;

/** Header, player count, player editor, time options, sound, start/rules. */
public final class SetupLayoutPlan {
    public final GameLayoutPlan.Box[] boxes=new GameLayoutPlan.Box[6];
    public final int columns;
    public SetupLayoutPlan(float width,float height,int players,boolean keyboard) {
        float pad=width<360?12:20, gap=6, edge=8, right=width-pad;
        boolean landscape=width>height;
        columns=width>=600 || (!landscape && height<580)?2:1;
        if(keyboard) {
            for(int i=0;i<6;i++) boxes[i]=new GameLayoutPlan.Box(pad,edge,right,edge);
            boxes[2]=new GameLayoutPlan.Box(pad,edge,right,height-edge-56-gap);
            boxes[5]=new GameLayoutPlan.Box(pad,height-edge-56,right,height-edge);
        } else if(landscape) {
            float mid=width*.5f;
            boxes[0]=new GameLayoutPlan.Box(pad,edge,right,edge+48);
            float top=edge+48+gap;
            boxes[1]=new GameLayoutPlan.Box(pad,top,mid-gap,top+48);
            boxes[2]=new GameLayoutPlan.Box(pad,top+48+gap,mid-gap,height-edge);
            boxes[3]=new GameLayoutPlan.Box(mid+gap,top,right,top+72);
            boxes[4]=new GameLayoutPlan.Box(mid+gap,top+72+gap,right,top+72+gap+48);
            boxes[5]=new GameLayoutPlan.Box(mid+gap,height-edge-56,right,height-edge);
        } else {
            float spare=Math.max(0,height-2*edge-5*gap-48-48-72-48-56);
            float desired=(float)Math.ceil(players/(double)columns)*56;
            float editor=Math.min(spare,desired);
            float extra=Math.max(0,spare-editor);
            float[] heights={48+extra*.35f,48,editor,72,48,56};
            float y=edge;
            for(int i=0;i<6;i++) {
                if(i==5)y=height-edge-56;
                boxes[i]=new GameLayoutPlan.Box(pad,y,right,y+heights[i]); y+=heights[i]+gap;
            }
        }
    }
}
