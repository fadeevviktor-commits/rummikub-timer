import ru.tileclock.GameLayoutPlan;

public class GameLayoutTest {
    private static void check(boolean b,String message) { if(!b)throw new AssertionError(message); }
    public static void main(String[] args) {
        int count=0;
        // Usable window dimensions after status/navigation bars, including split screen.
        for(int[] size:new int[][]{{320,426},{320,480},{360,640},{360,760},{384,800},{412,850},{600,900},{640,280},{760,320},{800,360},{900,600}}) {
            float w=size[0],h=size[1];
            GameLayoutPlan.Box[] boxes=GameLayoutPlan.arrange(w,h);
            for(int i=0;i<boxes.length;i++) {
                GameLayoutPlan.Box b=boxes[i];
                check(b.left>=0 && b.top>=0 && b.right<=w+.01 && b.bottom<=h+.01,"offscreen child "+i+" at "+w+"x"+h);
                check(b.right>b.left && b.bottom>b.top,"empty child "+i);
                for(int j=0;j<i;j++) {
                    GameLayoutPlan.Box a=boxes[j];
                    check(!(b.left<a.right-.01 && b.right>a.left+.01 && b.top<a.bottom-.01 && b.bottom>a.top+.01),"overlap");
                }
            }
            check(boxes[0].bottom-boxes[0].top>=48,"settings touch target");
            check(boxes[7].bottom-boxes[7].top>=48,"footer touch targets");
            check(boxes[4].bottom-boxes[4].top>=48,"timer touch target");
            count++;
        }
        System.out.println("PASS: all game controls fit in "+count+" portrait/landscape window sizes");
    }
}
