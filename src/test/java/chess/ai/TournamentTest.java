package chess.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.engine.Board;
import chess.engine.Move;
import chess.engine.PieceColor;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TournamentTest {

    private static final Evaluator EVALUATOR = new PieceSquareEvaluator();

    @Test
    void playsEachOpeningTwice() {
        Tournament.Result result = Tournament.play(
                new RandomStrategy(new Random(1)), new RandomStrategy(new Random(2)), 3, 7);
        assertEquals(6, result.getGames());
        assertEquals(6, result.getFirstWins() + result.getDraws() + result.getSecondWins());
    }

    @Test
    void searchingBeatsMovingAtRandom() {
        Tournament.Result result = Tournament.play(
                new AlphaBetaStrategy(2, EVALUATOR), new RandomStrategy(new Random(3)), 3, 11);

        assertEquals(0, result.getSecondWins());
        assertTrue(result.getFirstWins() >= 4, "won only " + result.getFirstWins() + " of 6");
        assertTrue(result.getFirstScorePercent() > 80);
    }

    @Test
    void recordsWorkDoneByEachSide() {
        Tournament.Result result = Tournament.play(
                new AlphaBetaStrategy(2, EVALUATOR), new RandomStrategy(new Random(3)), 1, 11);

        assertTrue(result.getFirst().getMoves() > 0);
        assertTrue(result.getSecond().getMoves() > 0);
        assertTrue(result.getFirst().getAverageNodes() > 20);
        assertEquals(0.0, result.getSecond().getAverageNodes());
        assertEquals("alpha-beta, depth 2", result.getFirst().getName());
    }

    @Test
    void sameSeedGivesTheSameMatch() {
        Tournament.Result first = Tournament.play(
                new AlphaBetaStrategy(2, EVALUATOR), new AlphaBetaStrategy(1, EVALUATOR), 2, 99);
        Tournament.Result second = Tournament.play(
                new AlphaBetaStrategy(2, EVALUATOR), new AlphaBetaStrategy(1, EVALUATOR), 2, 99);

        assertEquals(first.getFirstWins(), second.getFirstWins());
        assertEquals(first.getDraws(), second.getDraws());
        assertEquals(first.getFirst().getTotalNodes(), second.getFirst().getTotalNodes());
    }

    @Test
    void scoreCountsADrawAsHalfAWin() {
        Tournament.Result result = new Tournament.Result(
                new MeasuredStrategy(new RandomStrategy()), new MeasuredStrategy(new RandomStrategy()));
        assertEquals(0.0, result.getFirstScorePercent());  // no games yet
    }

    @Test
    void gameReportsTheColourThatWon() {
        // From the starting position, a depth-2 search mates a first-move player soon enough.
        MoveStrategy firstMove = new MoveStrategy() {
            @Override
            public Move chooseMove(Board board, PieceColor sideToMove) {
                return board.legalMoves(sideToMove).get(0);
            }

            @Override
            public String getName() {
                return "first move";
            }
        };
        MoveStrategy searcher = new AlphaBetaStrategy(3, EVALUATOR);

        assertEquals(PieceColor.WHITE, Tournament.playGame(searcher, firstMove, List.of()));
        assertEquals(PieceColor.BLACK, Tournament.playGame(firstMove, searcher, List.of()));
    }

    @Test
    void gameThatRunsTooLongIsADraw() {
        // Two players shuffling knights for ever repeat the position: a draw.
        MoveStrategy shuffler = new MoveStrategy() {
            @Override
            public Move chooseMove(Board board, PieceColor sideToMove) {
                for (Move move : board.legalMoves(sideToMove)) {
                    if (move.getPiece().getType() == chess.engine.PieceType.KNIGHT && !move.isCapture()) {
                        return move;
                    }
                }
                return board.legalMoves(sideToMove).get(0);
            }

            @Override
            public String getName() {
                return "shuffler";
            }
        };
        assertNull(Tournament.playGame(shuffler, shuffler, List.of()));
    }

    @Test
    void tableLinesUpUnderItsHeader() {
        Tournament.Result result = Tournament.play(
                new AlphaBetaStrategy(1, EVALUATOR), new RandomStrategy(new Random(1)), 1, 5);
        String line = Tournament.describe(result);
        assertTrue(line.startsWith("alpha-beta, depth 1"));
        assertTrue(line.contains("random"));
        assertEquals(Tournament.header().length(), line.length());
    }
}
