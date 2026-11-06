package chess.ui;

/** How much time each side gets on the clock: a starting amount, plus an increment for every move. */
public enum TimeControl {
    NONE("No clock", 0, 0),
    BULLET("1 min + 1 s", 1, 1),
    BLITZ("3 min + 2 s", 3, 2),
    RAPID("5 min + 3 s", 5, 3),
    CLASSICAL("10 min + 5 s", 10, 5);

    private final String label;
    private final int minutes;
    private final int incrementSeconds;

    TimeControl(String label, int minutes, int incrementSeconds) {
        this.label = label;
        this.minutes = minutes;
        this.incrementSeconds = incrementSeconds;
    }

    /** A new clock for this time control, or null if the game is not timed. */
    public ChessClock createClock() {
        return this == NONE ? null : new ChessClock(minutes * 60_000L, incrementSeconds * 1000L);
    }

    /** The name to show in a list of time controls. */
    @Override
    public String toString() {
        return label;
    }
}
