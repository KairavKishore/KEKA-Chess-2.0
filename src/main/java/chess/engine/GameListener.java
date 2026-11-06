package chess.engine;

/**
 * Something that wants to hear when a {@link Game} changes, such as a board
 * on screen that has to be redrawn. This is the Observer pattern: the game
 * knows only that it has listeners, not what they are, so the engine stays
 * free of any user interface code.
 *
 * <p>Register with {@link Game#addListener(GameListener)}.
 */
public interface GameListener {

    /**
     * Called after a move has been played, undone or redone. The game is
     * already in its new state. The call is made on whichever thread changed
     * the game.
     *
     * @param game the game that changed
     */
    void gameChanged(Game game);
}
