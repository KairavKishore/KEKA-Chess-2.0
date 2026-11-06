package chess.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.engine.Game;
import chess.engine.Move;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoveListTextTest {

    private static Game play(Game game, String... moves) {
        for (String text : moves) {
            Move found = null;
            for (Move move : game.legalMoves()) {
                if (move.toString().equals(text)) {
                    found = move;
                }
            }
            if (found == null) {
                throw new AssertionError("No legal move " + text);
            }
            game.makeMove(found);
        }
        return game;
    }

    @Test
    void emptyBeforeTheFirstMove() {
        assertTrue(MoveListText.lines(new Game().getMoveHistory()).isEmpty());
        assertEquals("", MoveListText.capturedBy(PieceColor.WHITE, new Game().getMoveHistory()));
    }

    @Test
    void oneNumberedLineForEachPairOfMoves() {
        Game game = play(new Game(), "e2e4", "e7e5", "g1f3", "b8c6");
        assertEquals(List.of("1. e2e4 e7e5", "2. g1f3 b8c6"), MoveListText.lines(game.getMoveHistory()));
    }

    @Test
    void lastLineIsHalfFullAfterWhitesMove() {
        Game game = play(new Game(), "e2e4", "e7e5", "g1f3");
        assertEquals(List.of("1. e2e4 e7e5", "2. g1f3"), MoveListText.lines(game.getMoveHistory()));
    }

    @Test
    void gameThatStartsWithBlackToMoveShowsDotsForWhite() {
        Game game = play(Game.fromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"),
                "e7e5", "g1f3", "b8c6");
        assertEquals(List.of("1. ... e7e5", "2. g1f3 b8c6"), MoveListText.lines(game.getMoveHistory()));
    }

    @Test
    void promotionShowsThePieceChosen() {
        Game game = play(Game.fromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1"), "a7a8n");
        assertEquals(List.of("1. a7a8n"), MoveListText.lines(game.getMoveHistory()));
    }

    @Test
    void capturedPiecesAreListedForTheSideThatTookThem() {
        Game game = play(new Game(), "e2e4", "d7d5", "e4d5", "d8d5", "b1c3", "d5g2", "f1g2");
        List<Move> history = game.getMoveHistory();

        // White took a pawn and then the queen: the queen is listed first.
        assertEquals(PieceGlyphs.of(PieceType.QUEEN) + PieceGlyphs.of(PieceType.PAWN),
                MoveListText.capturedBy(PieceColor.WHITE, history));
        assertEquals(PieceGlyphs.of(PieceType.PAWN) + PieceGlyphs.of(PieceType.PAWN),
                MoveListText.capturedBy(PieceColor.BLACK, history));
    }

    @Test
    void enPassantCountsAsACapturedPawn() {
        Game game = play(Game.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"), "e5d6");
        assertEquals(PieceGlyphs.of(PieceType.PAWN),
                MoveListText.capturedBy(PieceColor.WHITE, game.getMoveHistory()));
    }

    @Test
    void undoneMovesDropOutOfTheList() {
        Game game = play(new Game(), "e2e4", "d7d5", "e4d5");
        game.undo();
        assertEquals(List.of("1. e2e4 d7d5"), MoveListText.lines(game.getMoveHistory()));
        assertEquals("", MoveListText.capturedBy(PieceColor.WHITE, game.getMoveHistory()));
    }

    @Test
    void everyKindOfPieceHasItsOwnSymbol() {
        java.util.Set<String> symbols = new java.util.HashSet<>();
        for (PieceType type : PieceType.values()) {
            assertEquals(1, PieceGlyphs.of(type).length());
            symbols.add(PieceGlyphs.of(type));
        }
        assertEquals(PieceType.values().length, symbols.size());
    }
}
