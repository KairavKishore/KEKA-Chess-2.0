package chess.ai;

import chess.engine.Board;
import chess.engine.Piece;
import chess.engine.PieceColor;
import chess.engine.Position;

/**
 * Material plus piece-square tables: each piece scores its material value
 * and a small bonus or penalty for the square it stands on.
 *
 * <p>The tables encode general advice a player learns early. Knights belong
 * in the centre and are poor on the edge. Pawns gain value as they advance
 * and the centre pawns should move first. Rooks like the seventh rank. The
 * king should castle and stay behind its pawns. With material alone every
 * quiet opening move scores the same and the AI plays aimlessly; the tables
 * give it a reason to develop its pieces.
 *
 * <p>The numbers are Tomasz Michniewski's "Simplified Evaluation Function"
 * from the Chess Programming Wiki. There is one table for the king, tuned
 * for the middlegame; in an endgame the king ought to come to the centre,
 * which this evaluator does not know.
 */
public class PieceSquareEvaluator implements Evaluator {

    // Each table is written as the board is printed, from White's side: the
    // first row is rank 8 and the last is rank 1, with the a-file on the left.

    private static final int[][] PAWN = {
        {  0,   0,   0,   0,   0,   0,   0,   0},
        { 50,  50,  50,  50,  50,  50,  50,  50},
        { 10,  10,  20,  30,  30,  20,  10,  10},
        {  5,   5,  10,  25,  25,  10,   5,   5},
        {  0,   0,   0,  20,  20,   0,   0,   0},
        {  5,  -5, -10,   0,   0, -10,  -5,   5},
        {  5,  10,  10, -20, -20,  10,  10,   5},
        {  0,   0,   0,   0,   0,   0,   0,   0},
    };

    private static final int[][] KNIGHT = {
        {-50, -40, -30, -30, -30, -30, -40, -50},
        {-40, -20,   0,   0,   0,   0, -20, -40},
        {-30,   0,  10,  15,  15,  10,   0, -30},
        {-30,   5,  15,  20,  20,  15,   5, -30},
        {-30,   0,  15,  20,  20,  15,   0, -30},
        {-30,   5,  10,  15,  15,  10,   5, -30},
        {-40, -20,   0,   5,   5,   0, -20, -40},
        {-50, -40, -30, -30, -30, -30, -40, -50},
    };

    private static final int[][] BISHOP = {
        {-20, -10, -10, -10, -10, -10, -10, -20},
        {-10,   0,   0,   0,   0,   0,   0, -10},
        {-10,   0,   5,  10,  10,   5,   0, -10},
        {-10,   5,   5,  10,  10,   5,   5, -10},
        {-10,   0,  10,  10,  10,  10,   0, -10},
        {-10,  10,  10,  10,  10,  10,  10, -10},
        {-10,   5,   0,   0,   0,   0,   5, -10},
        {-20, -10, -10, -10, -10, -10, -10, -20},
    };

    private static final int[][] ROOK = {
        {  0,   0,   0,   0,   0,   0,   0,   0},
        {  5,  10,  10,  10,  10,  10,  10,   5},
        { -5,   0,   0,   0,   0,   0,   0,  -5},
        { -5,   0,   0,   0,   0,   0,   0,  -5},
        { -5,   0,   0,   0,   0,   0,   0,  -5},
        { -5,   0,   0,   0,   0,   0,   0,  -5},
        { -5,   0,   0,   0,   0,   0,   0,  -5},
        {  0,   0,   0,   5,   5,   0,   0,   0},
    };

    private static final int[][] QUEEN = {
        {-20, -10, -10,  -5,  -5, -10, -10, -20},
        {-10,   0,   0,   0,   0,   0,   0, -10},
        {-10,   0,   5,   5,   5,   5,   0, -10},
        { -5,   0,   5,   5,   5,   5,   0,  -5},
        {  0,   0,   5,   5,   5,   5,   0,  -5},
        {-10,   5,   5,   5,   5,   5,   0, -10},
        {-10,   0,   5,   0,   0,   0,   0, -10},
        {-20, -10, -10,  -5,  -5, -10, -10, -20},
    };

    private static final int[][] KING = {
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-20, -30, -30, -40, -40, -30, -30, -20},
        {-10, -20, -20, -20, -20, -20, -20, -10},
        { 20,  20,   0,   0,   0,   0,  20,  20},
        { 20,  30,  10,   0,   0,  10,  30,  20},
    };

    /** Indexed by {@code PieceType.ordinal()}: king, queen, rook, bishop, knight, pawn. */
    private static final int[][][] TABLES = {KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN};

    @Override
    public int evaluate(Board board) {
        int score = 0;
        for (int rank = 0; rank < Board.SIZE; rank++) {
            for (int file = 0; file < Board.SIZE; file++) {
                Piece piece = board.getPiece(new Position(file, rank));
                if (piece == null) {
                    continue;
                }
                boolean white = piece.getColor() == PieceColor.WHITE;
                // White reads the table as written: rank 8 (index 7) is row 0.
                // Black reads it upside down, so that "forward" means the same
                // for both sides.
                int row = white ? Board.SIZE - 1 - rank : rank;
                int value = MaterialEvaluator.valueOf(piece.getType())
                        + TABLES[piece.getType().ordinal()][row][file];
                score += white ? value : -value;
            }
        }
        return score;
    }
}
