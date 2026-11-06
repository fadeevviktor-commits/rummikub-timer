import ru.tileclock.PlayerOrder;
import ru.tileclock.TurnTimer;

public class PlayerOrderTest {
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void main(String[] args){
        for(int count=2;count<=4;count++) for(int first=0;first<count;first++) {
            for(int a=0;a<count;a++) for(int b=0;b<count;b++) {
                String[] names={"Анна","Миша","Катя","Саша"}; int[] faces={12,17,22,23};
                String selectedName=names[first]; int selectedFace=faces[first];
                int newFirst=PlayerOrder.swap(names,faces,first,a,b);
                check(names[newFirst].equals(selectedName) && faces[newFirst]==selectedFace,"selected player identity follows reorder");
                newFirst=PlayerOrder.swap(names,faces,newFirst,a,b);
                check(newFirst==first && names[0].equals("Анна") && faces[3]==23,"reverse reorder restores all data");
            }
            for(int seconds:new int[]{30,60,90,120}) {
                TurnTimer timer=new TurnTimer(count,seconds,first);
                check(timer.player==first && timer.state==TurnTimer.State.READY && timer.turn==1,"chosen player ready");
                timer.startOrNext(0); check(timer.player==first,"start keeps chosen player");
                for(int n=1;n<=count*2;n++) {
                    check(timer.advanceIfExpired(n*seconds*1000L),"automatic turn");
                    check(timer.player==(first+n)%count && timer.turn==n+1,"rotation from chosen player");
                }
            }
        }
        System.out.println("PASS: reordering keeps names, avatars and first player; all starting positions rotate correctly");
    }
}
