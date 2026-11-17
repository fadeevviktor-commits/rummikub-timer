package ru.tileclock;
/** Six visible groups; coordinates are usable-window dp after system insets. */
public final class GameLayoutPlan {
 public static final class Box {
  public final float left,top,right,bottom;
  Box(float l,float t,float r,float b){left=l;top=t;right=r;bottom=b;}
 }
 // Header, round/state, current identity, timer/action, next player, pause/resume.
 public static Box[] arrange(float width,float height) {
  Box[] b=new Box[6]; float pad=width<360?16:24,edge=8,gap=6,right=width-pad;
  if(width>height) {
   float middle=width*.43f,top=60,bottom=height-edge;
   b[0]=new Box(pad,edge,right,56);
   b[1]=new Box(pad,top,middle-gap,top+20);
   b[2]=new Box(pad,top+24,middle-gap,bottom-96);
   b[4]=new Box(pad,bottom-92,middle-gap,bottom-50);
   b[5]=new Box(pad,bottom-48,middle-gap,bottom);
   b[3]=new Box(middle+gap,top,right,bottom);
  } else {
   float[] heights={48,20,height<600?60:110,0,40,52};
   float fixed=2*edge+5*gap; for(float h:heights)fixed+=h;
   heights[3]=height-fixed; float y=edge;
   for(int i=0;i<6;i++){b[i]=new Box(pad,y,right,y+heights[i]);y+=heights[i]+gap;}
  }
  return b;
 }
}
