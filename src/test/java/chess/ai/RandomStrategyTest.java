package chess.ai;

import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.engine.Board;
import chess.engine.Game;
import chess.engine.Move;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RandomStrategyTest {

    @Test
    void choosesALegalMove() {
        Board board = Board.startingPosition();
        Move move = new RandomStrategy().chooseMove(board, WHITE);
        assertTrue(board.legalMoves(WHITE).contains(move));
    }

    @Test
    void choosesForTheSideItIsAskedAbout() {
        Board board = Board.startingPosition();
        Move move = new RandomStrategy().chooseMove(board, BLACK);
        assertEquals(BLACK, move.getPiece().getColor());
    }

    @Test
    void sameSeedGivesTheSameChoices() {
        Board board = Board.startingPosition();
        MoveStrategy first = new RandomStrategy(new Random(42));
        MoveStrategy second = new RandomStrategy(new Random(42));
        for (int i = 0; i < 20; i++) {
            assertEquals(first.chooseMove(board, WHITE), second.chooseMove(board, WHITE));
        }
    }

    @Test
    void everyLegalMoveGetsChosenEventually() {
        Board board = Board.startingPosition();
        MoveStrategy strategy = new RandomStrategy(new Random(1));
        Set<Move> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            seen.add(strategy.chooseMove(board, WHITE));
        }
        assertEquals(new HashSet<>(board.legalMoves(WHITE)), seen);
    }

    @Test
    void takesTheOnlyMoveWhenThereIsOne() {
        Game game = Game.fromFen("R5k1/5ppp/8/8/8/8/4K3/r7 b - - 0 1");  // must capture the checking rook
        Move move = new RandomStrategy().chooseMove(game.getBoard(), BLACK);
        assertEquals("a1a8", move.toString());
    }

    @Test
    void neverLeavesItsOwnKingInCheck() {
        Game game = Game.fromFen("4r1k1/8/8/8/8/8/4R3/4K3 w - - 0 1");  // the white rook is pinned
        MoveStrategy strategy = new RandomStrategy(new Random(7));
        for (int i = 0; i < 200; i++) {
            Board board = new Board(game.getBoard());
            board.makeMove(strategy.chooseMove(board, WHITE));
            assertTrue(!board.isInCheck(WHITE));
        }
    }

    @Test
    void refusesWhenThereIsNoLegalMove() {
        Game checkmated = Game.fromFen("R5k1/5ppp/8/8/8/8/8/4K3 b - - 0 1");
        assertThrows(IllegalStateException.class,
                () -> new RandomStrategy().chooseMove(checkmated.getBoard(), BLACK));
    }

    @Test
    void leavesTheBoardAsItFoundIt() {
        Game game = new Game();
        new RandomStrategy().chooseMove(game.getBoard(), WHITE);
        assertEquals(chess.engine.Fen.STARTING_POSITION, game.toFen());
    }

    @Test
    void hasAName() {
        assertEquals("random", new RandomStrategy().getName());
    }
}
