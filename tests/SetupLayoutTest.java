import ru.tileclock.SetupLayoutPlan;
import ru.tileclock.GameLayoutPlan;
public class SetupLayoutTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  int cases=0;
  for(int[] size:new int[][]{{320,426},{320,480},{360,568},{360,640},{384,740},{384,800},{412,850},{600,900},{640,280},{760,320},{900,600},{320,200},{360,300},{640,200}})
   for(int players=2;players<=4;players++)for(boolean keyboard:new boolean[]{false,true}){
    SetupLayoutPlan p=new SetupLayoutPlan(size[0],size[1],players,keyboard);
    for(int i=0;i<p.boxes.length;i++){
     GameLayoutPlan.Box b=p.boxes[i];check(b.left>=0 && b.right<=size[0] && b.bottom<=p.bodyHeight,"inside scroll content");
     check(b.bottom>=b.top,"no negative heights");if(i>0)check(b.top>=p.boxes[i-1].bottom,"no overlap");
    }
    check(p.boxes[2].bottom-p.boxes[2].top==players*68,"all ordered player rows reachable");
    check(p.footer.top>=p.viewport && p.footer.bottom<=size[1],"start visible below scroll content");
    check(p.footer.bottom-p.footer.top>=48,"start touch target");
    if(keyboard)check(p.boxes[0].top==p.boxes[0].bottom && p.boxes[3].top==p.boxes[3].bottom,"keyboard prioritizes player editor");
    cases++;
   }
  System.out.println("PASS: pinned start and scrollable single player list in "+cases+" window/player/keyboard cases");
 }
}
