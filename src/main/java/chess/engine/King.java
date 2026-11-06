package chess.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves one square in any direction, and can castle: once per game the king
 * goes two squares towards a rook, which jumps to the square the king crossed.
 */
public final class King extends Piece {

    /** The king starts on the e-file. */
    static final int HOME_FILE = 4;

    public King(PieceColor color) {
        super(color, PieceType.KING);
    }

    @Override
    public List<Move> pseudoLegalMoves(Board board, Position from) {
        List<Move> moves = new ArrayList<>();
        for (int[] direction : ALL_DIRECTIONS) {
            addStepMove(board, from, direction[0], direction[1], moves);
        }
        addCastlingMoves(board, from, moves);
        return moves;
    }

    @Override
    public boolean attacks(Board board, Position from, Position target) {
        int fileDistance = Math.abs(target.getFile() - from.getFile());
        int rankDistance = Math.abs(target.getRank() - from.getRank());
        return Math.max(fileDistance, rankDistance) == 1;
    }

    /**
     * Castling needs all of the following:
     * <ul>
     *   <li>neither the king nor that rook has moved (the board's castling right),</li>
     *   <li>every square between them is empty,</li>
     *   <li>the king is not in check, and</li>
     *   <li>the king does not pass through an attacked square.</li>
     * </ul>
     * The king may not land on an attacked square either. That is not checked
     * here, because it is true of every king move and the legal move filter
     * in {@link Board#legalMoves} already removes such moves.
     */
    private void addCastlingMoves(Board board, Position from, List<Move> moves) {
        PieceColor color = getColor();
        boolean kingSide = board.hasCastlingRight(color, CastlingSide.KING_SIDE);
        boolean queenSide = board.hasCastlingRight(color, CastlingSide.QUEEN_SIDE);
        if (!kingSide && !queenSide) {
            return;
        }
        int rank = Board.homeRank(color);
        if (!from.equals(new Position(HOME_FILE, rank)) || board.isSquareAttacked(from, color.opposite())) {
            return;
        }
        if (kingSide) {
            addCastlingMove(board, from, Board.SIZE - 1, 1, moves);
        }
        if (queenSide) {
            addCastlingMove(board, from, 0, -1, moves);
        }
    }

    /**
     * @param rookFile  the file of the rook to castle with
     * @param direction +1 towards the h-file, -1 towards the a-file
     */
    private void addCastlingMove(Board board, Position from, int rookFile, int direction, List<Move> moves) {
        int rank = from.getRank();
        if (!new Rook(getColor()).equals(board.getPiece(new Position(rookFile, rank)))) {
            return;
        }
        for (int file = from.getFile() + direction; file != rookFile; file += direction) {
            if (!board.isEmpty(new Position(file, rank))) {
                return;
            }
        }
        Position crossed = new Position(from.getFile() + direction, rank);
        if (board.isSquareAttacked(crossed, getColor().opposite())) {
            return;
        }
        moves.add(new Move(from, new Position(from.getFile() + 2 * direction, rank), this, null));
    }
}
