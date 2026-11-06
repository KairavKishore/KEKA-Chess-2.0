package chess.engine;

import java.util.List;
import java.util.Objects;

/**
 * Base class for the six kinds of chess piece.
 *
 * <p>A piece knows its colour and how it moves. It does not store where it
 * is: the {@link Board} is the only owner of piece locations, so the two can
 * never disagree. That also makes pieces immutable, so a copied board can
 * safely share the same piece objects.
 */
public abstract class Piece {

    /** The four directions along ranks and files. Directions are {file step, rank step}. */
    protected static final int[][] ORTHOGONAL = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
    /** The four diagonal directions. */
    protected static final int[][] DIAGONAL = {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}};
    /** All eight directions: the king's steps and the queen's lines. */
    protected static final int[][] ALL_DIRECTIONS = {
        {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}, {-1, -1}, {-1, 0}, {-1, 1}
    };

    private final PieceColor color;
    private final PieceType type;

    protected Piece(PieceColor color, PieceType type) {
        this.color = Objects.requireNonNull(color, "color");
        this.type = Objects.requireNonNull(type, "type");
    }

    /** The side this piece belongs to. */
    public PieceColor getColor() {
        return color;
    }

    /** Which of the six kinds of piece this is. */
    public PieceType getType() {
        return type;
    }

    /** Letter for printing: upper case for White, lower case for Black. */
    public char getSymbol() {
        char symbol = type.getSymbol();
        return color == PieceColor.WHITE ? symbol : Character.toLowerCase(symbol);
    }

    /**
     * Every move this piece can make from the given square: it follows the
     * piece's movement pattern, stops at blockers and captures enemy pieces.
     *
     * <p>"Pseudo-legal" means king safety is not considered. A move that
     * leaves the mover's own king in check is still returned, because a
     * single piece cannot know that; it depends on the whole board.
     *
     * @param board the board the piece is standing on
     * @param from  the square the piece is standing on
     */
    public abstract List<Move> pseudoLegalMoves(Board board, Position from);

    /**
     * True if this piece, standing on {@code from}, attacks {@code target}:
     * it could capture an enemy piece there.
     *
     * <p>This is not the same as having a move to the square. A pawn attacks
     * the two squares diagonally ahead even when they are empty, and does not
     * attack the square straight ahead that it can move to. It also does not
     * matter what stands on {@code target}.
     */
    public abstract boolean attacks(Board board, Position from, Position target);

    /**
     * Factory method: builds a piece from its type, for code that only has
     * the label (promotion now, reading a position from text later).
     */
    public static Piece create(PieceType type, PieceColor color) {
        switch (type) {
            case KING:
                return new King(color);
            case QUEEN:
                return new Queen(color);
            case ROOK:
                return new Rook(color);
            case BISHOP:
                return new Bishop(color);
            case KNIGHT:
                return new Knight(color);
            case PAWN:
                return new Pawn(color);
            default:
                throw new IllegalArgumentException("Unknown piece type: " + type);
        }
    }

    /** True if {@code other} is a piece of the opposite colour. */
    protected boolean isEnemy(Piece other) {
        return other != null && other.color != color;
    }

    /**
     * Adds the move that lands (fileStep, rankStep) away from {@code from},
     * provided that square is on the board and is empty or holds an enemy
     * piece. Used by the pieces that move a fixed distance (king, knight).
     */
    protected void addStepMove(Board board, Position from, int fileStep, int rankStep, List<Move> moves) {
        int file = from.getFile() + fileStep;
        int rank = from.getRank() + rankStep;
        if (!Position.isValid(file, rank)) {
            return;
        }
        Position to = new Position(file, rank);
        Piece occupant = board.getPiece(to);
        if (occupant == null || isEnemy(occupant)) {
            moves.add(new Move(from, to, this, occupant));
        }
    }

    /**
     * Factory method: builds a piece from the letter that {@link #getSymbol()}
     * prints, so 'N' is a white knight and 'n' a black one.
     *
     * @throws IllegalArgumentException if no piece uses that letter
     */
    public static Piece fromSymbol(char letter) {
        PieceColor color = Character.isUpperCase(letter) ? PieceColor.WHITE : PieceColor.BLACK;
        return create(PieceType.fromSymbol(letter), color);
    }

    /** Two pieces are equal if they are the same kind and colour. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Piece other = (Piece) obj;
        return color == other.color && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, type);
    }

    /** For example "WHITE KNIGHT". */
    @Override
    public String toString() {
        return color + " " + type;
    }
}
