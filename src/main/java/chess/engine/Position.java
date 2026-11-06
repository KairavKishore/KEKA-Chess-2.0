package chess.engine;

/**
 * A square on the board, such as "e4". Immutable.
 *
 * <p>Squares are addressed the way chess players do it, by file and rank:
 * <ul>
 *   <li>file 0..7 is a..h, left to right from White's side</li>
 *   <li>rank 0..7 is 1..8, so White's pieces start on ranks 0 and 1</li>
 * </ul>
 */
public final class Position {

    private final int file;
    private final int rank;

    /**
     * @throws IllegalArgumentException if the square is off the board
     */
    public Position(int file, int rank) {
        if (!isValid(file, rank)) {
            throw new IllegalArgumentException("Off the board: file " + file + ", rank " + rank);
        }
        this.file = file;
        this.rank = rank;
    }

    /**
     * Parses algebraic notation, for example {@code Position.of("e4")}.
     *
     * @throws IllegalArgumentException if the text is not a square from a1 to h8
     */
    public static Position of(String square) {
        if (square == null || square.length() != 2) {
            throw new IllegalArgumentException("Not a square: " + square);
        }
        int file = square.charAt(0) - 'a';
        int rank = square.charAt(1) - '1';
        if (!isValid(file, rank)) {
            throw new IllegalArgumentException("Not a square: " + square);
        }
        return new Position(file, rank);
    }

    /** True if (file, rank) lies on the board. */
    public static boolean isValid(int file, int rank) {
        return file >= 0 && file < Board.SIZE && rank >= 0 && rank < Board.SIZE;
    }

    /** The file, 0 to 7 for a to h. */
    public int getFile() {
        return file;
    }

    /** The rank, 0 to 7 for 1 to 8. */
    public int getRank() {
        return rank;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Position)) {
            return false;
        }
        Position other = (Position) obj;
        return file == other.file && rank == other.rank;
    }

    @Override
    public int hashCode() {
        return rank * Board.SIZE + file;
    }

    /** Algebraic notation, for example "e4". */
    @Override
    public String toString() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }
}
