package chess.ai;

import chess.engine.Board;

/**
 * Judges a position without looking ahead: a number saying who stands
 * better and by how much. A search calls this at the end of each line it
 * examines, so it runs thousands of times a move and has to be quick.
 */
public interface Evaluator {

    /**
     * Scores a position in centipawns (hundredths of a pawn) from White's
     * point of view: positive means White is better, negative means Black
     * is, and 0 is level. A score of 300 says White is about a knight ahead.
     */
    int evaluate(Board board);
}
