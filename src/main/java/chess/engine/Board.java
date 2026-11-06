package chess.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * The 8x8 board: which piece stands on which square.
 *
 * <p>The grid is private. Code outside the engine can look at the board and
 * ask for moves, but can only change it through {@link #makeMove(Move)} and
 * {@link #undoMove(Move)}.
 *
 * <p>Some rules depend on what happened earlier in the game and cannot be
 * read off the pieces alone, so the board remembers them: which castling
 * rights are left, the square a pawn has just skipped over (for en passant),
 * and how long it has been since a capture or pawn move (for the fifty-move
 * rule).
 */
public class Board {

    /** The number of ranks, and of files. */
    public static final int SIZE = 8;

    /** Indexed as squares[rank][file]. An empty square is null. */
    private final Piece[][] squares = new Piece[SIZE][SIZE];

    /**
     * Indexed as castlingRights[color][side]. A right is lost for good when
     * the king or that rook moves, or the rook is captured. Having the right
     * does not mean castling is possible at this moment.
     */
    private final boolean[][] castlingRights =
            new boolean[PieceColor.values().length][CastlingSide.values().length];

    /**
     * The square a pawn skipped with a double step on the last move, or null.
     * An enemy pawn may capture onto it, on the very next move only.
     */
    private Position enPassantTarget;

    /**
     * Moves by either side since the last capture or pawn move ("half-moves",
     * since chess players count one move as a turn by each side).
     */
    private int halfmoveClock;

    /** What {@link #undoMove(Move)} needs to take back each move played so far, newest first. */
    private final Deque<Snapshot> history = new ArrayDeque<>();

    /**
     * The part of the board's state that a move overwrites and that cannot
     * be worked out again from the move itself, saved just before the move
     * is played.
     */
    private static final class Snapshot {
        final Move move;
        final boolean[][] castlingRights;
        final Position enPassantTarget;
        final int halfmoveClock;

        Snapshot(Move move, Board board) {
            this.move = move;
            this.castlingRights = new boolean[board.castlingRights.length][];
            copyRights(board.castlingRights, castlingRights);
            this.enPassantTarget = board.enPassantTarget;
            this.halfmoveClock = board.halfmoveClock;
        }
    }

    private static void copyRights(boolean[][] source, boolean[][] target) {
        for (int color = 0; color < source.length; color++) {
            target[color] = source[color].clone();
        }
    }

    /** Creates an empty board. Nobody has castling rights on it. */
    public Board() {
    }

    /**
     * Copy constructor: the new board can be changed without affecting the
     * original. Pieces are immutable, so both boards share the piece objects.
     *
     * <p>The copy is the same position but starts with no undo history: moves
     * played on the original before copying cannot be undone on the copy.
     */
    public Board(Board other) {
        for (int rank = 0; rank < SIZE; rank++) {
            System.arraycopy(other.squares[rank], 0, squares[rank], 0, SIZE);
        }
        copyRights(other.castlingRights, castlingRights);
        enPassantTarget = other.enPassantTarget;
        halfmoveClock = other.halfmoveClock;
    }

    /** Creates a board with all 32 pieces on their starting squares. */
    public static Board startingPosition() {
        Board board = new Board();
        board.placeArmy(PieceColor.WHITE, 0, 1);
        board.placeArmy(PieceColor.BLACK, SIZE - 1, SIZE - 2);
        for (boolean[] rights : board.castlingRights) {
            Arrays.fill(rights, true);
        }
        return board;
    }

    /** The rank a side's pieces start on: 0 for White, 7 for Black. */
    static int homeRank(PieceColor color) {
        return color == PieceColor.WHITE ? 0 : SIZE - 1;
    }

    private void placeArmy(PieceColor color, int backRank, int pawnRank) {
        Piece[] backRow = {
            new Rook(color), new Knight(color), new Bishop(color), new Queen(color),
            new King(color), new Bishop(color), new Knight(color), new Rook(color)
        };
        for (int file = 0; file < SIZE; file++) {
            squares[backRank][file] = backRow[file];
            squares[pawnRank][file] = new Pawn(color);
        }
    }

    /** The piece on the given square, or null if the square is empty. */
    public Piece getPiece(Position position) {
        return squares[position.getRank()][position.getFile()];
    }

    /** True if no piece stands on the given square. */
    public boolean isEmpty(Position position) {
        return getPiece(position) == null;
    }

    /**
     * Puts a piece on a square, or clears the square if {@code piece} is null.
     * Package-private on purpose: only the engine itself (and its tests)
     * may place pieces directly.
     */
    void setPiece(Position position, Piece piece) {
        squares[position.getRank()][position.getFile()] = piece;
    }

    /** True if that side's king and rook have not moved, so castling that way is still allowed later. */
    public boolean hasCastlingRight(PieceColor color, CastlingSide side) {
        return castlingRights[color.ordinal()][side.ordinal()];
    }

    /** Package-private, like {@link #setPiece}: for setting up positions. */
    void setCastlingRight(PieceColor color, CastlingSide side, boolean allowed) {
        castlingRights[color.ordinal()][side.ordinal()] = allowed;
    }

    /** The square a pawn skipped with a double step on the last move, or null if there is none. */
    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    /** Package-private, like {@link #setPiece}: for setting up positions. */
    void setEnPassantTarget(Position target) {
        enPassantTarget = target;
    }

    /**
     * Moves by either side since the last capture or pawn move. At 100, which
     * is fifty moves each, the game is a draw.
     */
    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    /** Package-private, like {@link #setPiece}: for setting up positions. */
    void setHalfmoveClock(int halfmoveClock) {
        this.halfmoveClock = halfmoveClock;
    }

    /**
     * Plays a move on the board. Any piece on the destination square is
     * captured. Castling also moves the rook, en passant removes the pawn
     * beside the destination, and a promoting pawn is replaced by its new
     * piece.
     *
     * <p>This only checks that the piece is where the move says it is.
     * Whether the move is allowed at all is decided by {@link Game}.
     *
     * <p>The move can be taken back with {@link #undoMove(Move)}.
     *
     * @throws IllegalArgumentException if the moving piece is not on its starting square
     */
    public void makeMove(Move move) {
        Snapshot before = new Snapshot(move, this);
        play(move);
        history.push(before);
    }

    /**
     * Takes back the last move played, restoring the moved piece, anything it
     * captured, the castling rights, the en passant square and the half-move
     * clock. Moves must be undone in the reverse of the order they were
     * played.
     *
     * @throws IllegalStateException    if no move has been played on this board
     * @throws IllegalArgumentException if {@code move} is not the last move played
     */
    public void undoMove(Move move) {
        if (history.isEmpty()) {
            throw new IllegalStateException("There is no move to undo");
        }
        if (!history.peek().move.equals(move)) {
            throw new IllegalArgumentException("Not the last move played: " + move);
        }
        Snapshot before = history.pop();
        Position from = move.getFrom();
        Position to = move.getTo();

        // Putting back the piece the move started with also turns a promoted piece into a pawn again.
        setPiece(from, move.getPiece());
        if (move.isEnPassant()) {
            setPiece(to, null);
            setPiece(enPassantVictim(move), move.getCaptured());
        } else {
            setPiece(to, move.getCaptured());
        }
        if (move.isCastling()) {
            setPiece(castlingRookHome(move), getPiece(castlingRookDestination(move)));
            setPiece(castlingRookDestination(move), null);
        }

        copyRights(before.castlingRights, castlingRights);
        enPassantTarget = before.enPassantTarget;
        halfmoveClock = before.halfmoveClock;
    }

    /** The square of the pawn an en passant move captures: on the mover's starting rank, in the destination file. */
    private static Position enPassantVictim(Move move) {
        return new Position(move.getTo().getFile(), move.getFrom().getRank());
    }

    /** The corner the rook starts in when castling. */
    private static Position castlingRookHome(Move move) {
        boolean kingSide = move.getTo().getFile() > move.getFrom().getFile();
        return new Position(kingSide ? SIZE - 1 : 0, move.getFrom().getRank());
    }

    /** The square the king crosses when castling, which is where the rook ends up. */
    private static Position castlingRookDestination(Move move) {
        return new Position((move.getFrom().getFile() + move.getTo().getFile()) / 2, move.getFrom().getRank());
    }

    /** Changes the board for a move without recording how to undo it. */
    private void play(Move move) {
        Position from = move.getFrom();
        Position to = move.getTo();
        Piece piece = getPiece(from);
        if (!move.getPiece().equals(piece)) {
            throw new IllegalArgumentException("There is no " + move.getPiece() + " on " + from);
        }
        boolean capture = move.isEnPassant() || !isEmpty(to);

        if (move.isEnPassant()) {
            setPiece(enPassantVictim(move), null);
        }
        if (move.isCastling()) {
            setPiece(castlingRookDestination(move), getPiece(castlingRookHome(move)));
            setPiece(castlingRookHome(move), null);
        }
        setPiece(to, move.isPromotion() ? Piece.create(move.getPromotion(), piece.getColor()) : piece);
        setPiece(from, null);

        // A move from a king's or rook's home square means that piece has
        // moved; a move onto a rook's home square means the rook was captured.
        revokeCastlingRights(from);
        revokeCastlingRights(to);

        boolean doubleStep = piece.getType() == PieceType.PAWN && Math.abs(to.getRank() - from.getRank()) == 2;
        enPassantTarget = doubleStep
                ? new Position(from.getFile(), (from.getRank() + to.getRank()) / 2)
                : null;

        halfmoveClock = capture || piece.getType() == PieceType.PAWN ? 0 : halfmoveClock + 1;
    }

    /** Removes the castling rights that depend on the king or rook that starts on this square. */
    private void revokeCastlingRights(Position square) {
        for (PieceColor color : PieceColor.values()) {
            if (square.getRank() != homeRank(color)) {
                continue;
            }
            boolean[] rights = castlingRights[color.ordinal()];
            if (square.getFile() == King.HOME_FILE) {
                Arrays.fill(rights, false);
            } else if (square.getFile() == SIZE - 1) {
                rights[CastlingSide.KING_SIDE.ordinal()] = false;
            } else if (square.getFile() == 0) {
                rights[CastlingSide.QUEEN_SIDE.ordinal()] = false;
            }
        }
    }

    /** The square the given side's king is on, or null if it has no king (possible in test positions). */
    public Position findKing(PieceColor color) {
        Piece king = new King(color);
        for (int rank = 0; rank < SIZE; rank++) {
            for (int file = 0; file < SIZE; file++) {
                if (king.equals(squares[rank][file])) {
                    return new Position(file, rank);
                }
            }
        }
        return null;
    }

    /**
     * True if any piece of the given colour attacks the square, whatever is
     * standing on it. See {@link Piece#attacks} for how attacking differs
     * from being able to move there.
     */
    public boolean isSquareAttacked(Position square, PieceColor byColor) {
        for (int rank = 0; rank < SIZE; rank++) {
            for (int file = 0; file < SIZE; file++) {
                Piece piece = squares[rank][file];
                if (piece != null && piece.getColor() == byColor
                        && piece.attacks(this, new Position(file, rank), square)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True if the given side's king is attacked. */
    public boolean isInCheck(PieceColor color) {
        Position king = findKing(color);
        return king != null && isSquareAttacked(king, color.opposite());
    }

    /**
     * True if neither side has enough pieces left to give checkmate, however
     * badly the other plays: king against king, king and one bishop or knight
     * against king, or kings with bishops that all stand on squares of the
     * same colour.
     */
    public boolean hasInsufficientMaterial() {
        int others = 0;
        int knights = 0;
        boolean onlyBishops = true;
        boolean bishopOnLight = false;
        boolean bishopOnDark = false;
        for (int rank = 0; rank < SIZE; rank++) {
            for (int file = 0; file < SIZE; file++) {
                Piece piece = squares[rank][file];
                if (piece == null || piece.getType() == PieceType.KING) {
                    continue;
                }
                others++;
                if (piece.getType() == PieceType.KNIGHT) {
                    knights++;
                }
                if (piece.getType() != PieceType.BISHOP) {
                    onlyBishops = false;
                } else if ((rank + file) % 2 == 0) {
                    bishopOnDark = true;   // a1 is (0, 0) and is a dark square
                } else {
                    bishopOnLight = true;
                }
            }
        }
        if (others == 0) {
            return true;
        }
        if (onlyBishops) {
            return !(bishopOnLight && bishopOnDark);
        }
        return others == 1 && knights == 1;
    }

    /**
     * All legal moves for one colour: the pseudo-legal moves that do not
     * leave that side's own king in check.
     */
    public List<Move> legalMoves(PieceColor color) {
        return withoutSelfChecks(pseudoLegalMoves(color));
    }

    /** The legal moves of the piece on one square; empty if the square is empty. */
    public List<Move> legalMovesFrom(Position from) {
        return withoutSelfChecks(pseudoLegalMovesFrom(from));
    }

    /**
     * Tries each move on a copy of the board and keeps it only if the mover's
     * king is safe afterwards. This one test covers pinned pieces, kings
     * walking into attacks, and failing to answer a check.
     */
    private List<Move> withoutSelfChecks(List<Move> candidates) {
        List<Move> legal = new ArrayList<>();
        for (Move move : candidates) {
            Board next = new Board(this);
            next.play(move);  // the copy is thrown away, so there is nothing to undo
            if (!next.isInCheck(move.getPiece().getColor())) {
                legal.add(move);
            }
        }
        return legal;
    }

    /** All pseudo-legal moves for every piece of one colour. */
    public List<Move> pseudoLegalMoves(PieceColor color) {
        List<Move> moves = new ArrayList<>();
        for (int rank = 0; rank < SIZE; rank++) {
            for (int file = 0; file < SIZE; file++) {
                Piece piece = squares[rank][file];
                if (piece != null && piece.getColor() == color) {
                    // Polymorphism: the board does not know or care which
                    // kind of piece this is. Each subclass supplies its own moves.
                    moves.addAll(piece.pseudoLegalMoves(this, new Position(file, rank)));
                }
            }
        }
        return moves;
    }

    /** The pseudo-legal moves of the piece on one square; empty if the square is empty. */
    public List<Move> pseudoLegalMovesFrom(Position from) {
        Piece piece = getPiece(from);
        if (piece == null) {
            return new ArrayList<>();
        }
        return piece.pseudoLegalMoves(this, from);
    }

    /**
     * A text diagram seen from White's side. Upper-case letters are White's
     * pieces, lower-case are Black's, and a dot is an empty square.
     */
    @Override
    public String toString() {
        String border = "  +------------------------+\n";
        StringBuilder text = new StringBuilder(border);
        for (int rank = SIZE - 1; rank >= 0; rank--) {
            text.append(rank + 1).append(" |");
            for (int file = 0; file < SIZE; file++) {
                Piece piece = squares[rank][file];
                text.append(' ').append(piece == null ? '.' : piece.getSymbol()).append(' ');
            }
            text.append("|\n");
        }
        text.append(border);
        text.append("    a  b  c  d  e  f  g  h\n");
        return text.toString();
    }
}
