package chess.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.ai.MeasuredStrategy;
import chess.ai.MinimaxStrategy;
import chess.ai.PieceSquareEvaluator;
import chess.ai.RandomStrategy;
import chess.engine.Board;
import chess.engine.PieceColor;
import org.junit.jupiter.api.Test;

class StatsTextTest {

    @Test
    void saysSoBeforeTheFirstMove() {
        MeasuredStrategy strategy = new MeasuredStrategy(new MinimaxStrategy(2, new PieceSquareEvaluator()));
        assertEquals("White (minimax, depth 2): no moves yet", StatsText.describe("White", strategy));
    }

    @Test
    void reportsMovesTimeAndPositions() {
        MeasuredStrategy strategy = new MeasuredStrategy(new MinimaxStrategy(2, new PieceSquareEvaluator()));
        strategy.chooseMove(Board.startingPosition(), PieceColor.WHITE);
        strategy.chooseMove(Board.startingPosition(), PieceColor.WHITE);

        String text = StatsText.describe("Black", strategy);

        assertTrue(text.startsWith("Black (minimax, depth 2): 2 moves, average "), text);
        assertTrue(text.endsWith(" ms and 421 positions a move"), text);
    }

    @Test
    void saysWhenAPersonIsPlayingTheSide() {
        assertEquals("Black: played by a person", StatsText.describe("Black", null));
    }

    @Test
    void oneMoveIsSingular() {
        MeasuredStrategy strategy = new MeasuredStrategy(new MinimaxStrategy(1, new PieceSquareEvaluator()));
        strategy.chooseMove(Board.startingPosition(), PieceColor.WHITE);
        assertTrue(StatsText.describe("White", strategy).contains(": 1 move, "));
    }

    @Test
    void largeCountsAreGroupedInThousands() {
        MeasuredStrategy strategy = new MeasuredStrategy(new MinimaxStrategy(3, new PieceSquareEvaluator()));
        strategy.chooseMove(Board.startingPosition(), PieceColor.WHITE);
        assertTrue(StatsText.describe("White", strategy).contains(" and 9,323 positions a move"),
                StatsText.describe("White", strategy));
    }

    @Test
    void leavesPositionsOutForAStrategyThatDoesNotSearch() {
        MeasuredStrategy strategy = new MeasuredStrategy(new RandomStrategy());
        strategy.chooseMove(Board.startingPosition(), PieceColor.WHITE);
        String text = StatsText.describe("White", strategy);
        assertTrue(text.startsWith("White (random): 1 move, average "), text);
        assertTrue(text.endsWith(" ms a move"), text);
    }
}
