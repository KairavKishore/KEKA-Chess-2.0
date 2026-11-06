package chess.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Jumps in an L shape: two squares one way and one square sideways.
 * Pieces in between do not block it.
 */
public final class Knight extends Piece {

    private static final int[][] JUMPS = {
        {1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };

    public Knight(PieceColor color) {
        super(color, PieceType.KNIGHT);
    }

    @Override
    public List<Move> pseudoLegalMoves(Board board, Position from) {
        List<Move> moves = new ArrayList<>();
        for (int[] jump : JUMPS) {
            addStepMove(board, from, jump[0], jump[1], moves);
        }
        return moves;
    }

    @Override
    public boolean attacks(Board board, Position from, Position target) {
        int fileDistance = Math.abs(target.getFile() - from.getFile());
        int rankDistance = Math.abs(target.getRank() - from.getRank());
        return fileDistance * rankDistance == 2;  // 1 by 2 or 2 by 1
    }
}
