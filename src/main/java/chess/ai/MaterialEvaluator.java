package chess.ai;

import chess.engine.Board;
import chess.engine.Piece;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import chess.engine.Position;

/**
 * Counts material: the total value of White's pieces minus the total value
 * of Black's. It knows what is on the board but nothing about where.
 */
public class MaterialEvaluator implements Evaluator {

    /**
     * What each kind of piece is worth in centipawns. The king has no value
     * because it is never captured; losing it is checkmate, which the search
     * scores separately.
     */
    public static int valueOf(PieceType type) {
        switch (type) {
            case PAWN:
                return 100;
            case KNIGHT:
                return 320;
            case BISHOP:
                return 330;  // slightly more than a knight, so the AI will not swap one for the other
            case ROOK:
                return 500;
            case QUEEN:
                return 900;
            default:
                return 0;
        }
    }

    @Override
    public int evaluate(Board board) {
        int score = 0;
        for (int rank = 0; rank < Board.SIZE; rank++) {
            for (int file = 0; file < Board.SIZE; file++) {
                Piece piece = board.getPiece(new Position(file, rank));
                if (piece != null) {
                    int value = valueOf(piece.getType());
                    score += piece.getColor() == PieceColor.WHITE ? value : -value;
                }
            }
        }
        return score;
    }
}
