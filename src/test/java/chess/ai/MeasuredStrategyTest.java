package chess.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.engine.Board;
import chess.engine.PieceColor;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MeasuredStrategyTest {

    private static final Evaluator EVALUATOR = new PieceSquareEvaluator();

    @Test
    void startsWithNothingRecorded() {
        MeasuredStrategy measured = new MeasuredStrategy(new RandomStrategy());
        assertEquals(0, measured.getMoves());
        assertEquals(0.0, measured.getAverageMillis());
        assertEquals(0.0, measured.getAverageNodes());
        assertEquals(0, measured.getTotalNodes());
        assertEquals(0, measured.getNodesSearched());
    }

    @Test
    void choosesTheSameMovesAsTheStrategyItWraps() {
        Board board = Board.startingPosition();
        MeasuredStrategy measured = new MeasuredStrategy(new RandomStrategy(new Random(5)));
        MoveStrategy plain = new RandomStrategy(new Random(5));
        for (int i = 0; i < 10; i++) {
            assertEquals(plain.chooseMove(board, PieceColor.WHITE), measured.chooseMove(board, PieceColor.WHITE));
        }
        assertEquals(
                new AlphaBetaStrategy(3, EVALUATOR).chooseMove(board, PieceColor.WHITE),
                new MeasuredStrategy(new AlphaBetaStrategy(3, EVALUATOR)).chooseMove(board, PieceColor.WHITE));
    }

    @Test
    void takesItsNameFromTheStrategyItWraps() {
        assertEquals("alpha-beta, depth 3", new MeasuredStrategy(new AlphaBetaStrategy(3, EVALUATOR)).getName());
    }

    @Test
    void countsMovesAndPositions() {
        Board board = Board.startingPosition();
        MeasuredStrategy measured = new MeasuredStrategy(new MinimaxStrategy(2, EVALUATOR));

        measured.chooseMove(board, PieceColor.WHITE);
        assertEquals(1, measured.getMoves());
        assertEquals(421, measured.getNodesSearched());  // the root, 20 moves and 400 replies
        assertEquals(421.0, measured.getAverageNodes());

        measured.chooseMove(board, PieceColor.WHITE);
        assertEquals(2, measured.getMoves());
        assertEquals(842, measured.getTotalNodes());
        assertEquals(421.0, measured.getAverageNodes());
    }

    @Test
    void averagesOverMovesOfDifferentSizes() {
        MeasuredStrategy measured = new MeasuredStrategy(new MinimaxStrategy(1, EVALUATOR));
        measured.chooseMove(Board.startingPosition(), PieceColor.WHITE);  // 21 positions
        Board afterE4 = Board.startingPosition();
        afterE4.makeMove(afterE4.legalMovesFrom(chess.engine.Position.of("e2")).get(1));
        measured.chooseMove(afterE4, PieceColor.WHITE);                   // 31 positions

        assertEquals(31, measured.getNodesSearched());
        assertEquals(52, measured.getTotalNodes());
        assertEquals(26.0, measured.getAverageNodes());
    }

    @Test
    void randomStrategyExaminesNoPositions() {
        MeasuredStrategy measured = new MeasuredStrategy(new RandomStrategy());
        measured.chooseMove(Board.startingPosition(), PieceColor.WHITE);
        assertEquals(1, measured.getMoves());
        assertEquals(0, measured.getTotalNodes());
    }

    @Test
    void timesTheChoosingOfAMove() {
        MoveStrategy slow = new MoveStrategy() {
            @Override
            public chess.engine.Move chooseMove(Board board, PieceColor sideToMove) {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return board.legalMoves(sideToMove).get(0);
            }

            @Override
            public String getName() {
                return "slow";
            }
        };
        MeasuredStrategy measured = new MeasuredStrategy(slow);

        measured.chooseMove(Board.startingPosition(), PieceColor.WHITE);
        measured.chooseMove(Board.startingPosition(), PieceColor.WHITE);

        assertTrue(measured.getLastMillis() >= 29, "last: " + measured.getLastMillis());
        assertTrue(measured.getAverageMillis() >= 29, "average: " + measured.getAverageMillis());
        assertTrue(measured.getAverageMillis() < 5000);
    }
}
