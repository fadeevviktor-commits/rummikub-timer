package ru.tileclock;

/** All timestamps are monotonic milliseconds, never wall-clock time. */
public final class TurnTimer {
    public enum State { READY, RUNNING, PAUSED, EXPIRED }
    public int players, seconds, player, turn = 1;
    public State state = State.READY;
    private long deadline, remaining;
    public TurnTimer(int players, int seconds) {
        if (players < 2 || players > 4 || (seconds != 30 && seconds != 60 && seconds != 90 && seconds != 120))
            throw new IllegalArgumentException("Invalid game settings");
        this.players = players; this.seconds = seconds; remaining = seconds * 1000L;
    }
    public long remaining(long now) { return state == State.RUNNING ? Math.max(0, deadline - now) : remaining; }
    public int displayedSeconds(long now) { return (int)((remaining(now) + 999) / 1000); }
    public boolean update(long now) {
        if (state == State.RUNNING && remaining(now) == 0) { remaining = 0; state = State.EXPIRED; return true; }
        return false;
    }
    public boolean advanceIfExpired(long now) {
        if (update(now) || state == State.EXPIRED) {
            startOrNext(now);
            return true;
        }
        return false;
    }
    public void startOrNext(long now) {
        if (state == State.PAUSED) return;
        if (state != State.READY) { player = (player + 1) % players; turn++; }
        remaining = seconds * 1000L; deadline = now + remaining; state = State.RUNNING;
    }
    public void pause(long now) {
        if (state == State.RUNNING) { remaining = remaining(now); state = remaining == 0 ? State.EXPIRED : State.PAUSED; }
    }
    public void resume(long now) {
        if (state == State.PAUSED) { deadline = now + remaining; state = State.RUNNING; }
    }
    public void restore(int player, int turn, State state, long remaining) {
        this.player = Math.max(0, Math.min(players - 1, player)); this.turn = Math.max(1, turn);
        this.remaining = Math.max(0, Math.min(seconds * 1000L, remaining));
        this.state = state == State.RUNNING ? State.PAUSED : state;
        if (this.remaining == 0 && this.state == State.PAUSED) this.state = State.EXPIRED;
    }
}
