package chess.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.engine.Board;
import chess.engine.Game;
import chess.engine.PieceColor;
import org.junit.jupiter.api.Test;

class DifficultyTest {

    @Test
    void weakestLevelPlaysRandomMoves() {
        assertTrue(Difficulty.RANDOM.createStrategy() instanceof RandomStrategy);
        assertEquals(0, Difficulty.RANDOM.getDepth());
    }

    @Test
    void otherLevelsSearchDeeperAsTheyGetHarder() {
        int previous = 0;
        for (Difficulty level : new Difficulty[] {Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD}) {
            MoveStrategy strategy = level.createStrategy();
            assertTrue(strategy instanceof AlphaBetaStrategy, level.name());
            assertEquals(level.getDepth(), ((AlphaBetaStrategy) strategy).getDepth());
            assertTrue(level.getDepth() > previous, level.name());
            previous = level.getDepth();
        }
    }

    @Test
    void eachCallBuildsAFreshStrategy() {
        assertNotSame(Difficulty.HARD.createStrategy(), Difficulty.HARD.createStrategy());
    }

    @Test
    void levelsHaveReadableNames() {
        assertEquals("Random moves", Difficulty.RANDOM.toString());
        assertEquals("Easy", Difficulty.EASY.toString());
        assertEquals("Medium", Difficulty.MEDIUM.toString());
        assertEquals("Hard", Difficulty.HARD.toString());
    }

    @Test
    void everyLevelCanChooseAMoveFromTheStart() {
        for (Difficulty level : Difficulty.values()) {
            Board board = Board.startingPosition();
            assertTrue(board.legalMoves(PieceColor.WHITE)
                    .contains(level.createStrategy().chooseMove(board, PieceColor.WHITE)), level.name());
        }
    }

    @Test
    void easyBeatsRandomMoves() {
        // Not a proof of strength, but a searching player should never lose
        // to one that moves at random, and should usually mate it.
        int wins = 0;
        for (long seed = 1; seed <= 4; seed++) {
            Game game = new Game();
            MoveStrategy white = Difficulty.EASY.createStrategy();
            MoveStrategy black = new RandomStrategy(new java.util.Random(seed));
            while (!game.getStatus().isGameOver()) {
                MoveStrategy toMove = game.getTurn() == PieceColor.WHITE ? white : black;
                game.makeMove(toMove.chooseMove(new Board(game.getBoard()), game.getTurn()));
            }
            boolean whiteLost = game.getStatus() == chess.engine.GameStatus.CHECKMATE
                    && game.getTurn() == PieceColor.WHITE;
            assertTrue(!whiteLost, "lost to random moves with seed " + seed);
            if (game.getStatus() == chess.engine.GameStatus.CHECKMATE) {
                wins++;
            }
        }
        assertTrue(wins >= 1, "never managed to checkmate a random player");
    }
}
