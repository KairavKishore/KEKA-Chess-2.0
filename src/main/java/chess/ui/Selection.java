package chess.ui;

import chess.engine.Game;
import chess.engine.Move;
import chess.engine.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * What the player has picked up: the square they clicked first, and the
 * moves its piece can make. Turns a series of clicks into a choice of move,
 * without playing it.
 *
 * <p>This class has no JavaFX in it, so the click rules can be tested
 * without opening a window. {@link BoardView} forwards clicks here and draws
 * whatever this says is selected.
 */
public class Selection {

    private final BoardSource source;

    /** The square of the piece picked up, or null if nothing is selected. */
    private Position selected;

    /** The legal moves of the selected piece; empty if nothing is selected. */
    private List<Move> moves = new ArrayList<>();

    /** A selection for an ordinary game of chess. */
    public Selection(Game game) {
        this(new GameBoardSource(game));
    }

    /**
     * @param source says which pieces may be picked up and where they can go
     */
    public Selection(BoardSource source) {
        this.source = source;
    }

    /**
     * Handles a click on a square:
     * <ul>
     *   <li>on a square the selected piece can move to: the player has
     *       chosen that move, and the selection is cleared;</li>
     *   <li>on a piece that may be picked up: selects it, replacing any
     *       earlier selection;</li>
     *   <li>anywhere else, or on the selected piece again: clears the
     *       selection.</li>
     * </ul>
     *
     * @return the moves the click chose between: empty if it did not choose
     *         a move, one move normally, and four when a pawn promotes (one
     *         for each piece it could become, for the caller to ask about)
     */
    public List<Move> click(Position square) {
        List<Move> candidates = movesTo(square);
        if (!candidates.isEmpty()) {
            clear();
            return candidates;
        }

        if (source.canSelect(square) && !square.equals(selected)) {
            selected = square;
            moves = source.movesFrom(square);
        } else {
            clear();
        }
        return new ArrayList<>();
    }

    /** Forgets the selected piece. */
    public void clear() {
        selected = null;
        moves = new ArrayList<>();
    }

    /** The square of the selected piece, or null if nothing is selected. */
    public Position getSelected() {
        return selected;
    }

    /** The moves the selected piece could make that end on the given square. */
    public List<Move> movesTo(Position square) {
        List<Move> matching = new ArrayList<>();
        for (Move move : moves) {
            if (move.getTo().equals(square)) {
                matching.add(move);
            }
        }
        return matching;
    }
}
