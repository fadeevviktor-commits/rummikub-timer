import ru.tileclock.SetupLayoutPlan;
import ru.tileclock.GameLayoutPlan;

public class SetupLayoutTest {
    static void check(boolean b,String message) {if(!b)throw new AssertionError(message);}
    public static void main(String[] args) {
        int cases=0;
        for(int[] size:new int[][]{{320,426},{320,480},{360,568},{360,640},{384,800},{412,850},{600,900},{640,280},{760,320},{900,600}}) {
            for(int players=2;players<=4;players++) {
                SetupLayoutPlan p=new SetupLayoutPlan(size[0],size[1],players,false);
                for(int i=0;i<p.boxes.length;i++) {
                    GameLayoutPlan.Box b=p.boxes[i];
                    check(b.left>=0 && b.top>=0 && b.right<=size[0]+.01 && b.bottom<=size[1]+.01,"setup offscreen at "+size[0]+"x"+size[1]);
                    check(b.right>b.left && b.bottom>b.top,"empty control");
                    for(int j=0;j<i;j++) {
                        GameLayoutPlan.Box a=p.boxes[j];
                        check(!(b.left<a.right-.01 && b.right>a.left+.01 && b.top<a.bottom-.01 && b.bottom>a.top+.01),"overlap");
                    }
                }
                check(p.boxes[1].bottom-p.boxes[1].top>=47.99,"player count touch target");
                check(p.boxes[5].bottom-p.boxes[5].top>=47.99,"start button touch target");
                int rows=(players+p.columns-1)/p.columns;
                check(p.boxes[2].bottom-p.boxes[2].top>=rows*56-6-.01,"all player rows visible");
                cases++;
            }
        }
        for(int[] size:new int[][]{{320,200},{360,300},{640,200}}) {
            SetupLayoutPlan p=new SetupLayoutPlan(size[0],size[1],4,true);
            check(p.boxes[2].bottom>p.boxes[2].top,"keyboard editor viewport");
            check(p.boxes[5].bottom<=size[1] && p.boxes[5].bottom-p.boxes[5].top>=47.99,"start above keyboard");
            check(p.boxes[2].bottom<=p.boxes[5].top,"keyboard no overlap");
        }
        System.out.println("PASS: setup fits "+cases+" player/window combinations plus 3 keyboard sizes");
    }
}
