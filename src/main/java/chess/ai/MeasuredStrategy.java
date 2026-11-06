package chess.ai;

import chess.engine.Board;
import chess.engine.Move;
import chess.engine.PieceColor;

/**
 * Wraps another strategy and keeps count of the work it does: how many moves
 * it has chosen, how long they took and how many positions it examined.
 *
 * <p>This is the Decorator pattern. It is itself a {@link MoveStrategy} and
 * chooses exactly the moves the wrapped one would, so it can be dropped in
 * anywhere without the strategy or its user knowing they are being measured.
 *
 * <p>The figures should be read from the thread that receives the chosen
 * move, after it has arrived.
 */
public class MeasuredStrategy implements MoveStrategy {

    private static final double NANOS_PER_MILLI = 1_000_000.0;

    private final MoveStrategy strategy;

    private int moves;
    private long totalNanos;
    private long totalNodes;
    private long lastNanos;
    private long lastNodes;

    /**
     * @param strategy the strategy to measure
     */
    public MeasuredStrategy(MoveStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public Move chooseMove(Board board, PieceColor sideToMove) {
        long started = System.nanoTime();
        Move move = strategy.chooseMove(board, sideToMove);
        lastNanos = System.nanoTime() - started;
        lastNodes = strategy.getNodesSearched();

        moves++;
        totalNanos += lastNanos;
        totalNodes += lastNodes;
        return move;
    }

    @Override
    public String getName() {
        return strategy.getName();
    }

    @Override
    public long getNodesSearched() {
        return lastNodes;
    }

    /** How many moves have been chosen. */
    public int getMoves() {
        return moves;
    }

    /** How long the last move took to choose, in milliseconds. */
    public double getLastMillis() {
        return lastNanos / NANOS_PER_MILLI;
    }

    /** The average time to choose a move, in milliseconds; 0 before the first move. */
    public double getAverageMillis() {
        return moves == 0 ? 0 : totalNanos / NANOS_PER_MILLI / moves;
    }

    /** The average number of positions examined per move; 0 before the first move. */
    public double getAverageNodes() {
        return moves == 0 ? 0 : (double) totalNodes / moves;
    }

    /** The total number of positions examined over all moves. */
    public long getTotalNodes() {
        return totalNodes;
    }
}
