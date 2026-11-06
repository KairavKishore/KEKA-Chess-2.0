package chess.ui;

import chess.cheat.CheatGame.SquareState;
import chess.engine.Move;
import chess.engine.Piece;
import chess.engine.PieceType;
import chess.engine.Position;

import java.util.List;

/**
 * What a {@link BoardView} needs to know in order to draw a game and let
 * someone choose a move in it. Ordinary chess and cheating chess each
 * supply one, so the same board draws both.
 */
public interface BoardSource {

    /** The piece to draw on a square, or null if it is empty. */
    Piece pieceAt(Position position);

    /** True if the square holds a piece the person at the board may pick up now. */
    boolean canSelect(Position position);

    /** The moves the piece on a square may make. Only asked for squares that can be selected. */
    List<Move> movesFrom(Position from);

    /** The move just played, to be highlighted, or null. */
    Move getLastMove();

    /** A square to mark as in danger, the king's when it is in check, or null. */
    Position getAlarmSquare();

    /** A word or two to announce the end of the game, or null while it is still going. */
    String getBanner();

    /**
     * What the piece on a square secretly is, if that is not what it looks
     * like and the person at the board is allowed to know; null otherwise.
     * Only cheating chess has secrets.
     */
    default PieceType secretAt(Position position) {
        return null;
    }

    /** What has happened to a square: a crater, lava, or made safe. Ordinary chess has only normal squares. */
    default SquareState stateAt(Position position) {
        return SquareState.NORMAL;
    }

    /** How hard it is storming, from 1 to 5, or 0 if the weather is fine. */
    default int getStormSeverity() {
        return 0;
    }

    /**
     * True if a move on offer would give its piece away, because the piece
     * could not make it if it were what it looks like. Such moves are
     * marked in red and the rest in green. Ordinary chess has no such
     * moves and marks all of them plainly.
     */
    default boolean isCheatingMove(Move move) {
        return false;
    }

    /** True if moves should be marked green or red according to {@link #isCheatingMove}. */
    default boolean marksCheatingMoves() {
        return false;
    }

    /** True if fog should hide the middle of the board. */
    default boolean isFoggy() {
        return false;
    }

    /** True if every piece should be drawn the same colour, so the sides cannot be told apart. */
    default boolean isBlob() {
        return false;
    }

    /** True if half of the board should be blacked out. */
    default boolean isHalfDark() {
        return false;
    }

    /** True if a click should land on the opposite square of the board. */
    default boolean isBackwards() {
        return false;
    }
}
