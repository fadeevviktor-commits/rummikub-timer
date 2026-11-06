import ru.tileclock.SetupLayoutPlan;
import ru.tileclock.GameLayoutPlan;

public class SetupLayoutTest {
    static void check(boolean b,String message) {if(!b)throw new AssertionError(message);}
    public static void main(String[] args) {
        int cases=0;
        for(int[] size:new int[][]{{320,426},{320,480},{360,568},{360,640},{384,740},{384,800},{412,850},{600,900},{640,280},{760,320},{900,600}}) {
            for(int players=2;players<=4;players++) {
                SetupLayoutPlan p=new SetupLayoutPlan(size[0],size[1],players,false);
                int[] body={0,1,2,6,3,4};
                for(int i=0;i<body.length;i++) {
                    GameLayoutPlan.Box b=p.boxes[body[i]];
                    check(b.left>=0 && b.top>=0 && b.right<=size[0]+.01 && b.bottom<=p.bodyHeight+.01,"body control out of scroll content");
                    check(b.right>b.left && b.bottom>b.top,"empty control");
                    for(int j=0;j<i;j++) {
                        GameLayoutPlan.Box a=p.boxes[body[j]];
                        check(!(b.left<a.right-.01 && b.right>a.left+.01 && b.top<a.bottom-.01 && b.bottom>a.top+.01),"body controls overlap");
                    }
                }
                check(p.boxes[1].bottom-p.boxes[1].top>=47.99,"player count touch target");
                check(p.boxes[6].bottom-p.boxes[6].top>=63.99,"first player selector and hint");
                GameLayoutPlan.Box footer=p.boxes[5];
                check(footer.bottom<=size[1] && footer.top>=p.viewport,"start pinned below scroll viewport");
                check(footer.bottom-footer.top>=48,"start touch target");
                int rows=(players+p.columns-1)/p.columns;
                check(p.boxes[2].bottom-p.boxes[2].top>=rows*56-6-.01,"all player rows in form");
                if(size[1]>=640 && size[0]<size[1])check(p.bodyHeight<=p.viewport+.01,"ordinary portrait form needs no scrolling");
                cases++;
            }
        }
        for(int[] size:new int[][]{{320,200},{360,300},{640,200}}) {
            SetupLayoutPlan p=new SetupLayoutPlan(size[0],size[1],4,true);
            check(p.boxes[2].bottom>p.boxes[2].top && p.boxes[2].bottom<=p.bodyHeight,"keyboard editor viewport");
            check(p.boxes[5].bottom<=size[1] && p.boxes[5].bottom-p.boxes[5].top>=48,"start above keyboard");
        }
        System.out.println("PASS: setup layout in "+cases+" player/window combinations and 3 keyboard sizes");
    }
}
