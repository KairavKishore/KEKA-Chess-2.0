package chess.engine;

import java.util.Objects;

/**
 * One move: a piece going from one square to another, possibly capturing.
 * Immutable.
 *
 * <p>A move remembers the piece it captured, which is exactly the
 * information needed to take the move back later.
 *
 * <p>The three special moves are described like this:
 * <ul>
 *   <li><b>Castling</b> is the king's move of two squares. The rook's move is
 *       implied, and {@link Board#makeMove(Move)} plays it.</li>
 *   <li><b>En passant</b> is built with {@link #enPassant}, because the
 *       captured pawn is not on the destination square.</li>
 *   <li><b>Promotion</b> carries the kind of piece the pawn turns into, so a
 *       pawn reaching the last rank has four different moves to the same
 *       square.</li>
 * </ul>
 */
public final class Move {

    private final Position from;
    private final Position to;
    private final Piece piece;
    private final Piece captured;
    private final PieceType promotion;
    private final boolean enPassant;

    /**
     * An ordinary move or capture, which also covers castling (the king's
     * two-square move).
     *
     * @param piece    the piece being moved
     * @param captured the piece taken on {@code to}, or null if nothing is captured
     */
    public Move(Position from, Position to, Piece piece, Piece captured) {
        this(from, to, piece, captured, null, false);
    }

    /**
     * A pawn move to the last rank.
     *
     * @param promotion the kind of piece the pawn becomes
     */
    public Move(Position from, Position to, Piece piece, Piece captured, PieceType promotion) {
        this(from, to, piece, captured, promotion, false);
    }

    private Move(Position from, Position to, Piece piece, Piece captured, PieceType promotion, boolean enPassant) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.piece = Objects.requireNonNull(piece, "piece");
        this.captured = captured;
        this.promotion = promotion;
        this.enPassant = enPassant;
    }

    /**
     * An en passant capture: the pawn lands on the empty square {@code to},
     * and the captured pawn stands beside the square it started from.
     *
     * @param captured the enemy pawn being taken
     */
    public static Move enPassant(Position from, Position to, Piece piece, Piece captured) {
        return new Move(from, to, piece, Objects.requireNonNull(captured, "captured"), null, true);
    }

    /** The square the piece starts on. */
    public Position getFrom() {
        return from;
    }

    /** The square the piece ends on. */
    public Position getTo() {
        return to;
    }

    /** The piece being moved. */
    public Piece getPiece() {
        return piece;
    }

    /** The piece this move captures, or null if it is not a capture. */
    public Piece getCaptured() {
        return captured;
    }

    /** True if this move takes a piece, including en passant. */
    public boolean isCapture() {
        return captured != null;
    }

    /** The kind of piece a pawn becomes on the last rank, or null if this is not a promotion. */
    public PieceType getPromotion() {
        return promotion;
    }

    /** True if a pawn reaches the last rank and becomes another piece. */
    public boolean isPromotion() {
        return promotion != null;
    }

    /** True if this is an en passant capture. */
    public boolean isEnPassant() {
        return enPassant;
    }

    /** True if this is castling: the only time a king moves two squares. */
    public boolean isCastling() {
        return piece.getType() == PieceType.KING && Math.abs(to.getFile() - from.getFile()) == 2;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Move)) {
            return false;
        }
        Move other = (Move) obj;
        return from.equals(other.from)
                && to.equals(other.to)
                && piece.equals(other.piece)
                && Objects.equals(captured, other.captured)
                && promotion == other.promotion
                && enPassant == other.enPassant;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, piece, captured, promotion, enPassant);
    }

    /** Coordinate notation, for example "e2e4", or "e7e8q" for a promotion to a queen. */
    @Override
    public String toString() {
        String text = from.toString() + to;
        if (promotion != null) {
            text += Character.toLowerCase(promotion.getSymbol());
        }
        return text;
    }
}
