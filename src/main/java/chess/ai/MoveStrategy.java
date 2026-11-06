package chess.ai;

import chess.engine.Board;
import chess.engine.Move;
import chess.engine.PieceColor;

/**
 * A way of choosing a move. This is the Strategy pattern: each algorithm
 * (random, minimax, alpha-beta) is a class implementing this interface, and
 * the code that plays the move works with any of them.
 */
public interface MoveStrategy {

    /**
     * Chooses a move for one side.
     *
     * <p>The board belongs to the strategy for the length of the call. It may
     * play and undo moves on it freely, as long as it leaves the position as
     * it found it. This method may take a long time and must not touch any
     * user interface, because it is called on a background thread.
     *
     * @param board      the position to move in, which has at least one legal move
     * @param sideToMove the side to choose for
     * @return one of {@code board.legalMoves(sideToMove)}
     * @throws IllegalStateException if the side has no legal moves
     */
    Move chooseMove(Board board, PieceColor sideToMove);

    /** A short name to show the user, such as "random". */
    String getName();

    /**
     * How many positions the last call to {@link #chooseMove} examined, as a
     * measure of the work it did. A strategy that does not look ahead leaves
     * this at 0.
     */
    default long getNodesSearched() {
        return 0;
    }
}
