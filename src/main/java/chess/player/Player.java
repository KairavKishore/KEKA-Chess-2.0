package chess.player;

import chess.engine.Game;
import chess.engine.Move;

import java.util.function.Consumer;

/**
 * One side of a game: something that can be asked for a move. A person
 * answers by clicking on the board, a computer by running an algorithm.
 * Whoever runs the game treats them the same.
 */
public interface Player {

    /** What to call this player on screen. */
    String getName();

    /** True if a person is choosing the moves, false if a program is. */
    boolean isHuman();

    /**
     * Asks the player to choose a move in the game's current position, where
     * it is this player's turn and the game is not over.
     *
     * <p>This returns straight away. The player answers later, when the
     * person has clicked or the computer has finished thinking, by calling
     * {@code reply} once with a legal move. The player must not play the
     * move on the game itself.
     *
     * @param game  the game to choose a move in
     * @param reply where to send the chosen move
     */
    void requestMove(Game game, Consumer<Move> reply);
}
