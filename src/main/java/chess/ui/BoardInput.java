package chess.ui;

import chess.engine.Move;
import chess.engine.Position;

import java.util.function.Consumer;

/**
 * The ways a person can answer through the board: choosing a move, or
 * pointing at squares. {@link BoardView} is the real one; tests use a
 * stand-in, so that the flow of a game can be checked without a window.
 */
public interface BoardInput {

    /**
     * Lets the person choose a move by clicking a piece and then a square,
     * and sends it to {@code reply}. Passing null stops asking.
     */
    void requestMove(Consumer<Move> reply);

    /**
     * Sends every square the person clicks to {@code picker}, instead of
     * treating clicks as moves. Passing null goes back to moves.
     */
    void pickSquares(Consumer<Position> picker);

    /** Marks one square as picked, or none if null. */
    void highlight(Position position);

    /** Draws the board again after the game has changed. */
    void refresh();
}
