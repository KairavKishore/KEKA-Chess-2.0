package chess.ui;

import chess.ai.MeasuredStrategy;

import java.util.Locale;

/** The wording of the computer players' statistics. Kept apart from JavaFX so it can be tested without a window. */
final class StatsText {

    private StatsText() {
    }

    /**
     * One line about who is playing a side and, for a computer, how hard it
     * is working, for example
     * "White (alpha-beta, depth 4): 12 moves, average 35 ms and 5,300 positions a move".
     *
     * @param side     "White" or "Black"
     * @param strategy the strategy playing that side, or null if a person is
     */
    static String describe(String side, MeasuredStrategy strategy) {
        if (strategy == null) {
            return side + ": played by a person";
        }
        String who = side + " (" + strategy.getName() + "): ";
        if (strategy.getMoves() == 0) {
            return who + "no moves yet";
        }
        String moves = strategy.getMoves() == 1 ? "1 move" : strategy.getMoves() + " moves";
        String time = String.format(Locale.ROOT, "average %,.0f ms", strategy.getAverageMillis());
        // A strategy that does not search has no positions to report.
        String positions = strategy.getTotalNodes() == 0 ? ""
                : String.format(Locale.ROOT, " and %,.0f positions", strategy.getAverageNodes());
        return who + moves + ", " + time + positions + " a move";
    }
}
