package chess.ui;

import chess.engine.PieceColor;

import java.util.Locale;

/**
 * A chess clock: each side has a store of time that runs down while it is
 * that side's turn, and gains a fixed increment for every move it makes.
 * A side whose time reaches zero has lost.
 *
 * <p>The clock does not measure time itself. Whoever owns it says how much
 * has passed, which keeps it free of JavaFX and easy to test.
 */
public class ChessClock {

    private static final long MILLIS_PER_SECOND = 1000;
    private static final long SECONDS_PER_MINUTE = 60;

    /** Below this much time, tenths of a second are shown. */
    private static final long SHOW_TENTHS_BELOW = 10 * MILLIS_PER_SECOND;

    private final long[] remaining;
    private final long increment;

    /**
     * @param startMillis     the time each side begins with
     * @param incrementMillis the time a side gains for each move it makes
     */
    public ChessClock(long startMillis, long incrementMillis) {
        this.remaining = new long[] {startMillis, startMillis};
        this.increment = incrementMillis;
    }

    /** Runs a side's clock down by the time that has passed. It stops at zero. */
    public void advance(PieceColor running, long millis) {
        add(running, -millis);
    }

    /** A side has made a move: it gains the increment, unless its time had already run out. */
    public void moveMade(PieceColor mover) {
        if (!isOutOfTime(mover)) {
            add(mover, increment);
        }
    }

    /** Adds time to a side's clock, or takes it away if negative. It cannot go below zero. */
    public void add(PieceColor color, long millis) {
        remaining[color.ordinal()] = Math.max(0, remaining[color.ordinal()] + millis);
    }

    /** The time a side has left, in milliseconds. */
    public long getRemaining(PieceColor color) {
        return remaining[color.ordinal()];
    }

    public boolean isOutOfTime(PieceColor color) {
        return remaining[color.ordinal()] == 0;
    }

    /** A time as minutes and seconds, such as "4:07", with tenths in the last ten seconds: "0:07.3". */
    public static String format(long millis) {
        long minutes = millis / (SECONDS_PER_MINUTE * MILLIS_PER_SECOND);
        long seconds = millis / MILLIS_PER_SECOND % SECONDS_PER_MINUTE;
        if (millis < SHOW_TENTHS_BELOW) {
            return String.format(Locale.ROOT, "%d:%02d.%d", minutes, seconds, millis % MILLIS_PER_SECOND / 100);
        }
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }
}
