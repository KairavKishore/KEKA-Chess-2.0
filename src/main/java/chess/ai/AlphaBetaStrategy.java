package chess.ai;

import chess.engine.Board;
import chess.engine.Move;
import chess.engine.PieceColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Minimax with alpha-beta pruning: the same answer as
 * {@link MinimaxStrategy} at the same depth, found by looking at far fewer
 * positions.
 *
 * <p>The idea: once I have found a move that guarantees me some score, and
 * I see that another move lets my opponent hold me to less, I can stop
 * looking at that other move. It does not matter how much worse it could
 * get; I will not play it. The search carries two bounds to spot this:
 * <ul>
 *   <li><b>alpha</b>: the best score the side to move is already sure of;</li>
 *   <li><b>beta</b>: the most the opponent will allow. Any move scoring
 *       this or more will never be reached, because the opponent has a
 *       better alternative earlier on.</li>
 * </ul>
 * When alpha meets beta, the rest of the moves in that position are skipped.
 *
 * <p>Pruning works best when good moves are tried first, because bounds
 * tighten sooner. So the moves are ordered before searching: captures first,
 * the most valuable victim taken by the least valuable attacker ahead of
 * the rest. Ordering changes how much is skipped, never the score.
 */
public class AlphaBetaStrategy extends SearchStrategy {

    private final boolean orderMoves;

    /** Alpha-beta with move ordering, which is how it should normally be used. */
    public AlphaBetaStrategy(int depth, Evaluator evaluator) {
        this(depth, evaluator, true);
    }

    /**
     * @param orderMoves whether to try captures first. Turning it off shows
     *                   how much of the saving comes from ordering.
     */
    public AlphaBetaStrategy(int depth, Evaluator evaluator, boolean orderMoves) {
        super(depth, evaluator);
        this.orderMoves = orderMoves;
    }

    @Override
    protected int searchFromRoot(Board board, PieceColor sideToMove) {
        return alphaBeta(board, sideToMove, getDepth(), 0, -INFINITY, INFINITY);
    }

    /**
     * @param depth how many more half-moves to look ahead
     * @param ply   how many half-moves have been played from the root
     * @param alpha the score the side to move is already sure of
     * @param beta  the score at which the opponent would avoid this position
     * @return the score of the position for the side to move. It is exact
     *         when it lies between alpha and beta; outside them it is only a
     *         bound, which is all the caller needs.
     */
    private int alphaBeta(Board board, PieceColor sideToMove, int depth, int ply, int alpha, int beta) {
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
            // The opponent's bounds are ours, swapped and negated: what we
            // are sure of (alpha) is the most they can be allowed (their beta).
            int score = -alphaBeta(board, sideToMove.opposite(), depth - 1, ply + 1, -beta, -alpha);
            board.undoMove(move);

            if (score > best) {
                best = score;
                if (ply == 0) {
                    foundBetterRootMove(move);
                }
            }
            if (best > alpha) {
                alpha = best;
            }
            if (alpha >= beta) {
                break;  // the opponent will never let us get here: skip the remaining moves
            }
        }
        return anyLegalMove ? best : scoreNoMoves(board, sideToMove, ply);
    }

    @Override
    protected List<Move> candidateMoves(Board board, PieceColor sideToMove) {
        List<Move> moves = super.candidateMoves(board, sideToMove);
        if (!orderMoves) {
            return moves;
        }
        List<Move> ordered = new ArrayList<>(moves);
        // The sort is stable, so moves of equal priority keep their order
        // and the search stays repeatable.
        ordered.sort(Comparator.comparingInt(AlphaBetaStrategy::priority).reversed());
        return ordered;
    }

    /**
     * How promising a move looks before searching it; higher is tried
     * sooner. Captures score ten times the victim's value minus the
     * attacker's, so pawn takes queen comes before queen takes queen, and
     * both come before queen takes pawn. Promotions rank with captures.
     * Quiet moves score 0.
     */
    static int priority(Move move) {
        int priority = 0;
        if (move.isCapture()) {
            priority += 10 * MaterialEvaluator.valueOf(move.getCaptured().getType())
                    - MaterialEvaluator.valueOf(move.getPiece().getType());
        }
        if (move.isPromotion()) {
            priority += MaterialEvaluator.valueOf(move.getPromotion());
        }
        return priority;
    }

    @Override
    public String getName() {
        return "alpha-beta, depth " + getDepth();
    }
}
