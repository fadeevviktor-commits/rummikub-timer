import ru.tileclock.TurnTimer;

public class TurnTimerTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        for (int players=2;players<=4;players++) for (int duration:new int[]{30,60,90,120}) {
            TurnTimer t=new TurnTimer(players,duration);
            check(t.state==TurnTimer.State.READY && t.displayedSeconds(0)==duration,"ready");
            t.startOrNext(1000);
            check(t.player==0 && t.turn==1,"first turn");
            check(t.displayedSeconds(1001)==duration,"round up");
            check(t.displayedSeconds(2000)==duration-1,"countdown");
            t.pause(2500); long left=t.remaining(2500);
            check(t.state==TurnTimer.State.PAUSED && t.remaining(99000)==left,"pause freezes time");
            t.resume(100000);
            check(t.remaining(100001)==left-1,"resume preserves time");
            check(t.update(100000+left),"expiration event");
            check(!t.update(100001+left),"expiration fires once");
            check(t.displayedSeconds(Long.MAX_VALUE)==0 && t.player==0,"expiration holds player");
            for(int turn=1;turn<=players*3;turn++) {
                t.startOrNext(200000+turn*1000);
                check(t.player==turn%players && t.turn==turn+1,"rotation");
            }
            TurnTimer restored=new TurnTimer(players,duration);
            restored.restore(t.player,t.turn,t.state,t.remaining(203001));
            check(restored.state==TurnTimer.State.PAUSED,"restore pauses running game");
            TurnTimer auto=new TurnTimer(players,duration);
            auto.startOrNext(0);
            check(!auto.advanceIfExpired(duration*1000L-1),"no early auto advance");
            check(auto.advanceIfExpired(duration*1000L),"auto advance on zero");
            check(auto.player==1 && auto.turn==2 && auto.state==TurnTimer.State.RUNNING,"next player starts automatically");
            check(auto.displayedSeconds(duration*1000L)==duration,"next player has full time");
            check(!auto.advanceIfExpired(duration*1000L+80),"no repeated expiration signal");
            for(int n=2;n<=players;n++) {
                check(auto.advanceIfExpired(n*duration*1000L),"subsequent auto advance");
                check(auto.player==n%players,"auto wraps to first player");
            }
            auto.pause(players*duration*1000L+100);
            check(!auto.advanceIfExpired(Long.MAX_VALUE),"paused game never auto advances");
        }
        boolean rejected=false;
        try { new TurnTimer(1,45); } catch(IllegalArgumentException e) { rejected=true; }
        check(rejected,"invalid setup rejected");
        System.out.println("PASS: all 12 player/time combinations, countdown, pause/resume, expiration, rotation and restore");
    }
}
