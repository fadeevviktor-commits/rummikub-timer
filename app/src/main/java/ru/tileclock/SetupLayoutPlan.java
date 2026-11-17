package ru.tileclock;
/** One ordered list in every orientation, with a pinned start action. */
public final class SetupLayoutPlan {
 public final GameLayoutPlan.Box[] boxes=new GameLayoutPlan.Box[4];
 public final GameLayoutPlan.Box footer;
 public final float bodyHeight,viewport;
 public SetupLayoutPlan(float width,float height,int players,boolean keyboard) {
  float pad=width<360?16:24; viewport=Math.max(0,height-80);
  footer=new GameLayoutPlan.Box(pad,height-70,width-pad,height-14);
  float[] heights={keyboard?0:116,keyboard?0:50,players*68,keyboard?0:112};
  float y=8;
  for(int i=0;i<4;i++){boxes[i]=new GameLayoutPlan.Box(pad,y,width-pad,y+heights[i]);y+=heights[i];}
  bodyHeight=Math.max(viewport,y+8);
 }
}
