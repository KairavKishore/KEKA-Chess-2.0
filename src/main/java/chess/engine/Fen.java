package chess.engine;

/**
 * Reads and writes positions in FEN (Forsyth-Edwards Notation), the standard
 * one-line text form of a chess position. The starting position is
 *
 * <pre>rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1</pre>
 *
 * <p>The six fields, separated by spaces, are:
 * <ol>
 *   <li>the pieces, rank 8 first, ranks separated by '/'. Upper-case letters
 *       are White's pieces, lower-case are Black's, and a digit is that many
 *       empty squares in a row;</li>
 *   <li>the side to move, {@code w} or {@code b};</li>
 *   <li>the castling rights still held, from {@code KQkq}, or {@code -};</li>
 *   <li>the en passant target square, or {@code -};</li>
 *   <li>the half-move clock for the fifty-move rule;</li>
 *   <li>the move number, which starts at 1 and goes up after Black's move.</li>
 * </ol>
 */
public final class Fen {

    /** The standard starting position, White to move. */
    public static final String STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /** The letter for each castling right, in the order FEN lists them. */
    private static final String CASTLING_LETTERS = "KQkq";

    private Fen() {
    }

    /**
     * Builds a game from a FEN string. The last two fields may be left out,
     * in which case the clocks start at 0 and 1.
     *
     * @throws IllegalArgumentException if the text is not valid FEN
     */
    public static Game parse(String fen) {
        if (fen == null) {
            throw new IllegalArgumentException("FEN is null");
        }
        String[] fields = fen.trim().split("\\s+");
        if (fields.length != 4 && fields.length != 6) {
            throw new IllegalArgumentException("FEN needs 4 or 6 fields but has " + fields.length + ": " + fen);
        }

        Board board = new Board();
        placePieces(board, fields[0]);
        PieceColor turn = parseTurn(fields[1]);
        setCastlingRights(board, fields[2]);
        board.setEnPassantTarget(parseEnPassantTarget(fields[3]));

        int fullmoveNumber = 1;
        if (fields.length == 6) {
            board.setHalfmoveClock(parseNumber(fields[4], 0, "half-move clock"));
            fullmoveNumber = parseNumber(fields[5], 1, "move number");
        }
        return new Game(board, turn, fullmoveNumber);
    }

    private static void placePieces(Board board, String placement) {
        String[] ranks = placement.split("/", -1);
        if (ranks.length != Board.SIZE) {
            throw new IllegalArgumentException("FEN needs 8 ranks but has " + ranks.length + ": " + placement);
        }
        for (int row = 0; row < Board.SIZE; row++) {
            int rank = Board.SIZE - 1 - row;  // FEN lists rank 8 first
            int file = 0;
            for (char letter : ranks[row].toCharArray()) {
                if (letter >= '1' && letter <= '8') {
                    file += letter - '0';
                } else {
                    Piece piece = Piece.fromSymbol(letter);
                    if (file < Board.SIZE) {
                        board.setPiece(new Position(file, rank), piece);
                    }
                    file++;
                }
            }
            if (file != Board.SIZE) {
                throw new IllegalArgumentException(
                        "FEN rank " + (rank + 1) + " describes " + file + " squares, not 8: " + ranks[row]);
            }
        }
    }

    private static PieceColor parseTurn(String field) {
        if (field.equals("w")) {
            return PieceColor.WHITE;
        }
        if (field.equals("b")) {
            return PieceColor.BLACK;
        }
        throw new IllegalArgumentException("FEN side to move must be w or b: " + field);
    }

    private static void setCastlingRights(Board board, String field) {
        if (field.equals("-")) {
            return;
        }
        for (char letter : field.toCharArray()) {
            int index = CASTLING_LETTERS.indexOf(letter);
            if (index < 0) {
                throw new IllegalArgumentException("FEN castling rights must be - or letters from KQkq: " + field);
            }
            board.setCastlingRight(castlingColor(index), castlingSide(index), true);
        }
    }

    private static PieceColor castlingColor(int index) {
        return index < 2 ? PieceColor.WHITE : PieceColor.BLACK;
    }

    private static CastlingSide castlingSide(int index) {
        return index % 2 == 0 ? CastlingSide.KING_SIDE : CastlingSide.QUEEN_SIDE;
    }

    private static Position parseEnPassantTarget(String field) {
        if (field.equals("-")) {
            return null;
        }
        Position target = Position.of(field);
        // A pawn's double step always skips a square on rank 3 or rank 6.
        if (target.getRank() != 2 && target.getRank() != Board.SIZE - 3) {
            throw new IllegalArgumentException("FEN en passant square must be on rank 3 or 6: " + field);
        }
        return target;
    }

    private static int parseNumber(String field, int minimum, String name) {
        try {
            int number = Integer.parseInt(field);
            if (number >= minimum) {
                return number;
            }
        } catch (NumberFormatException e) {
            // reported below
        }
        throw new IllegalArgumentException("FEN " + name + " must be a number from " + minimum + " up: " + field);
    }

    /** The game's current position as a FEN string. */
    public static String format(Game game) {
        Board board = game.getBoard();
        return positionKey(board, game.getTurn())
                + " " + board.getHalfmoveClock()
                + " " + game.getFullmoveNumber();
    }

    /**
     * The first four FEN fields: everything that decides which moves are
     * possible, without the two counters. Two positions with the same key
     * count as the same position for the repetition rule.
     */
    static String positionKey(Board board, PieceColor turn) {
        StringBuilder text = new StringBuilder();
        for (int rank = Board.SIZE - 1; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < Board.SIZE; file++) {
                Piece piece = board.getPiece(new Position(file, rank));
                if (piece == null) {
                    empty++;
                    continue;
                }
                if (empty > 0) {
                    text.append(empty);
                    empty = 0;
                }
                text.append(piece.getSymbol());
            }
            if (empty > 0) {
                text.append(empty);
            }
            if (rank > 0) {
                text.append('/');
            }
        }
        text.append(turn == PieceColor.WHITE ? " w " : " b ");

        int lengthBeforeCastling = text.length();
        for (int index = 0; index < CASTLING_LETTERS.length(); index++) {
            if (board.hasCastlingRight(castlingColor(index), castlingSide(index))) {
                text.append(CASTLING_LETTERS.charAt(index));
            }
        }
        if (text.length() == lengthBeforeCastling) {
            text.append('-');
        }

        Position enPassantTarget = board.getEnPassantTarget();
        text.append(' ').append(enPassantTarget == null ? "-" : enPassantTarget.toString());
        return text.toString();
    }
}
