package chess.ui;

import chess.engine.Game;
import chess.engine.Move;
import chess.engine.PieceColor;
import chess.player.Player;

/**
 * Runs a game between two {@link Player}s: asks whoever's turn it is for a
 * move, plays it, and asks the other, until the game is over.
 *
 * <p>The controller does not care whether a player is a person or a
 * computer, or how long they take to answer. All of its methods, and the
 * players' replies, must be on the same thread (in the application, the
 * JavaFX thread), because {@link Game} is not thread-safe.
 */
public class GameController {

    private final Game game;
    private final Player white;
    private final Player black;

    /**
     * Goes up with every request and when the controller is stopped. A reply
     * is only acted on if it answers the latest request, so a computer that
     * finishes thinking after its game was abandoned changes nothing.
     */
    private int requestNumber;

    public GameController(Game game, Player white, Player black) {
        this.game = game;
        this.white = white;
        this.black = black;
    }

    /** The player whose turn it is. */
    public Player getPlayerToMove() {
        return game.getTurn() == PieceColor.WHITE ? white : black;
    }

    /** Begins play by asking the side to move for its move. */
    public void start() {
        requestNextMove();
    }

    /** Abandons the game: any move a player is still choosing will be ignored. */
    public void stop() {
        requestNumber++;
    }

    /**
     * True if there is a move to take back that a person made. Moves made by
     * a computer cannot be undone by themselves, because it would only make
     * another.
     */
    public boolean canUndo() {
        for (Move move : game.getMoveHistory()) {
            if (playerOf(move).isHuman()) {
                return true;
            }
        }
        return false;
    }

    /** True if a move that was taken back can be replayed. */
    public boolean canRedo() {
        return game.canRedo() && (white.isHuman() || black.isHuman());
    }

    /**
     * Takes back the last move a person made, together with any computer
     * moves played since, so that it is that person's turn again. If the
     * computer is thinking, its answer is dropped.
     *
     * @throws IllegalStateException if {@link #canUndo()} is false
     */
    public void undo() {
        if (!canUndo()) {
            throw new IllegalStateException("There is no move of a person's to undo");
        }
        stop();
        do {
            game.undo();
        } while (!getPlayerToMove().isHuman());
        requestNextMove();
    }

    /**
     * Replays the move last taken back, and the computer's reply to it if
     * that was taken back as well.
     *
     * @throws IllegalStateException if {@link #canRedo()} is false
     */
    public void redo() {
        if (!canRedo()) {
            throw new IllegalStateException("There is no move to redo");
        }
        stop();
        do {
            game.redo();
        } while (game.canRedo() && !getPlayerToMove().isHuman());
        requestNextMove();
    }

    private Player playerOf(Move move) {
        return move.getPiece().getColor() == PieceColor.WHITE ? white : black;
    }

    private void requestNextMove() {
        if (game.getStatus().isGameOver()) {
            return;
        }
        int thisRequest = ++requestNumber;
        getPlayerToMove().requestMove(game, move -> playReply(thisRequest, move));
    }

    private void playReply(int request, Move move) {
        if (request != requestNumber) {
            return;
        }
        game.makeMove(move);  // tells the game's listeners, which redraws the board
        requestNextMove();
    }
}
