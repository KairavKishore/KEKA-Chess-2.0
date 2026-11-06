package chess.ui;

import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.engine.Game;
import chess.engine.Move;
import chess.engine.PieceType;
import chess.engine.Position;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The click rules of the board, tested without a window. */
class SelectionTest {

    private Game game = new Game();
    private Selection selection = new Selection(game);

    private void startFrom(String fen) {
        game = Game.fromFen(fen);
        selection = new Selection(game);
    }

    /**
     * Clicks a square. If that chooses a move, plays it the way the game
     * controller would and returns it; otherwise returns null.
     */
    private Move click(String square) {
        List<Move> chosen = selection.click(Position.of(square));
        if (chosen.isEmpty()) {
            return null;
        }
        game.makeMove(chosen.get(0));
        return chosen.get(0);
    }

    private boolean isTarget(String square) {
        return !selection.movesTo(Position.of(square)).isEmpty();
    }

    @Test
    void nothingIsSelectedAtFirst() {
        assertNull(selection.getSelected());
        assertTrue(selection.movesTo(Position.of("e4")).isEmpty());
    }

    @Test
    void clickingAPieceSelectsItAndOffersItsMoves() {
        assertNull(click("e2"));

        assertEquals(Position.of("e2"), selection.getSelected());
        assertTrue(isTarget("e3"));
        assertTrue(isTarget("e4"));
        assertTrue(!isTarget("e5"));
        assertEquals(WHITE, game.getTurn());  // nothing has been played yet
    }

    @Test
    void clickingATargetChoosesTheMoveWithoutPlayingItAndClearsTheSelection() {
        click("e2");
        List<Move> chosen = selection.click(Position.of("e4"));

        assertEquals(1, chosen.size());
        assertEquals("e2e4", chosen.get(0).toString());
        assertEquals(WHITE, game.getTurn());  // playing it is the controller's job
        assertNull(game.getLastMove());
        assertNull(selection.getSelected());
        assertTrue(!isTarget("e3"));
    }

    @Test
    void clickingAnotherOfYourPiecesSwitchesToIt() {
        click("e2");
        click("g1");

        assertEquals(Position.of("g1"), selection.getSelected());
        assertTrue(isTarget("f3"));
        assertTrue(!isTarget("e4"));
    }

    @Test
    void clickingTheSelectedPieceAgainPutsItDown() {
        click("e2");
        click("e2");
        assertNull(selection.getSelected());
        assertTrue(!isTarget("e4"));
    }

    @Test
    void clickingASquareThePieceCannotReachPutsItDown() {
        click("e2");
        assertNull(click("e5"));
        assertNull(selection.getSelected());
        assertEquals(WHITE, game.getTurn());
    }

    @Test
    void theOtherSidesPiecesAndEmptySquaresCannotBeSelected() {
        click("e7");
        assertNull(selection.getSelected());
        click("e4");
        assertNull(selection.getSelected());
    }

    @Test
    void aPieceWithNoMovesCanBePickedUpButHasNowhereToGo() {
        click("a1");
        assertEquals(Position.of("a1"), selection.getSelected());
        assertTrue(!isTarget("a2"));
    }

    @Test
    void clickingAnEnemyPieceInRangeCapturesIt() {
        click("e2");
        click("e4");
        click("d7");
        click("d5");
        click("e4");
        Move capture = click("d5");

        assertNotNull(capture);
        assertTrue(capture.isCapture());
        assertEquals(WHITE, game.getBoard().getPiece(Position.of("d5")).getColor());
    }

    @Test
    void movesThatWouldLeaveTheKingInCheckAreNotOffered() {
        startFrom("4r1k1/8/8/8/8/8/4R3/4K3 w - - 0 1");  // the white rook is pinned
        click("e2");
        assertTrue(isTarget("e5"));
        assertTrue(!isTarget("d2"));
    }

    @Test
    void castlingIsPlayedByMovingTheKingTwoSquares() {
        startFrom("4k3/8/8/8/8/8/8/4K2R w K - 0 1");
        click("e1");
        Move castle = click("g1");

        assertTrue(castle.isCastling());
        assertEquals(PieceType.ROOK, game.getBoard().getPiece(Position.of("f1")).getType());
    }

    @Test
    void enPassantIsOfferedOnTheEmptySquareBehindThePawn() {
        startFrom("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        click("e5");
        Move capture = click("d6");

        assertTrue(capture.isEnPassant());
        assertNull(game.getBoard().getPiece(Position.of("d5")));
    }

    @Test
    void promotionOffersAChoiceOfFourMoves() {
        startFrom("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        click("a7");

        List<Move> choices = selection.click(Position.of("a8"));

        assertEquals(4, choices.size());
        assertEquals(PieceType.QUEEN, choices.get(0).getPromotion());
        assertEquals(PieceType.ROOK, choices.get(1).getPromotion());
        assertEquals(PieceType.BISHOP, choices.get(2).getPromotion());
        assertEquals(PieceType.KNIGHT, choices.get(3).getPromotion());
        assertNull(selection.getSelected());
    }

    @Test
    void nothingCanBeSelectedOnceTheGameIsOver() {
        startFrom("R5k1/5ppp/8/8/8/8/8/4K3 b - - 0 1");  // Black is checkmated
        click("g8");
        assertNull(selection.getSelected());

        startFrom("8/8/8/4k3/8/8/4K3/8 w - - 0 1");      // drawn: only kings
        click("e2");
        assertNull(selection.getSelected());
    }

    @Test
    void clearForgetsTheSelection() {
        click("e2");
        selection.clear();
        assertNull(selection.getSelected());
        assertNull(click("e4"));  // no longer a target, just an empty square
        assertEquals(WHITE, game.getTurn());
    }
}
