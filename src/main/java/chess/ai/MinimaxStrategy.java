package chess.ai;

import chess.engine.Board;
import chess.engine.Move;
import chess.engine.PieceColor;

/**
 * Minimax: looks at every sequence of moves to a fixed depth and assumes
 * that both sides always choose the move that is best for themselves.
 *
 * <p>The score of a position is the best score among its moves, where the
 * score of a move is whatever the opponent can then hold us to. At the
 * bottom, positions are scored by the {@link Evaluator}.
 *
 * <p>It is simple and correct but slow. A typical position has about 35
 * moves, so each extra half-move of depth multiplies the work by 35.
 * {@link AlphaBetaStrategy} gets the same answer while skipping most of it.
 */
public class MinimaxStrategy extends SearchStrategy {

    public MinimaxStrategy(int depth, Evaluator evaluator) {
        super(depth, evaluator);
    }

    @Override
    protected int searchFromRoot(Board board, PieceColor sideToMove) {
        return minimax(board, sideToMove, getDepth(), 0);
    }

    /**
     * @param depth how many more half-moves to look ahead
     * @param ply   how many half-moves have been played from the root
     * @return the score of the position for the side to move
     */
    private int minimax(Board board, PieceColor sideToMove, int depth, int ply) {
        countNode();
        if (ply > 0 && isDrawn(board)) {
            return 0;
        }
        if (depth == 0) {
            return scoreLeaf(board, sideToMove, ply);
        }

        int best = -INFINITY;
        boolean anyLegalMove = false;
        for (Move move : candidateMoves(board, sideToMove)) {
            if (!playIfLegal(board, move, sideToMove)) {
                continue;
            }
            anyLegalMove = true;
            // The opponent's best score is our worst, so negate it.
            int score = -minimax(board, sideToMove.opposite(), depth - 1, ply + 1);
            board.undoMove(move);

            if (score > best) {
                best = score;
                if (ply == 0) {
                    foundBetterRootMove(move);
                }
            }
        }
        return anyLegalMove ? best : scoreNoMoves(board, sideToMove, ply);
    }

    @Override
    public String getName() {
        return "minimax, depth " + getDepth();
    }
}
