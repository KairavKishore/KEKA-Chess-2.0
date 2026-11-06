package chess.engine;

import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoardTest {

    /** Plays the move between two squares, failing the test if no such move exists. */
    private static void play(Board board, String from, String to) {
        for (Move move : board.pseudoLegalMovesFrom(Position.of(from))) {
            if (move.getTo().equals(Position.of(to))) {
                board.makeMove(move);
                return;
            }
        }
        throw new AssertionError("No move from " + from + " to " + to);
    }

    @Test
    void newBoardIsEmpty() {
        Board board = new Board();
        for (int file = 0; file < Board.SIZE; file++) {
            for (int rank = 0; rank < Board.SIZE; rank++) {
                assertTrue(board.isEmpty(new Position(file, rank)));
            }
        }
    }

    @Test
    void startingPositionHasEveryPieceOnItsSquare() {
        Board board = Board.startingPosition();
        String backRow = "RNBQKBNR";
        for (int file = 0; file < Board.SIZE; file++) {
            char letter = backRow.charAt(file);
            assertEquals(letter, board.getPiece(new Position(file, 0)).getSymbol());
            assertEquals('P', board.getPiece(new Position(file, 1)).getSymbol());
            assertEquals('p', board.getPiece(new Position(file, 6)).getSymbol());
            assertEquals(Character.toLowerCase(letter), board.getPiece(new Position(file, 7)).getSymbol());
            for (int rank = 2; rank <= 5; rank++) {
                assertTrue(board.isEmpty(new Position(file, rank)));
            }
        }
    }

    @Test
    void queensStartOnTheDFileAndKingsOnTheEFile() {
        Board board = Board.startingPosition();
        assertEquals(new Queen(WHITE), board.getPiece(Position.of("d1")));
        assertEquals(new King(WHITE), board.getPiece(Position.of("e1")));
        assertEquals(new Queen(BLACK), board.getPiece(Position.of("d8")));
        assertEquals(new King(BLACK), board.getPiece(Position.of("e8")));
    }

    @Test
    void eachSideHasTwentyMovesAtTheStart() {
        Board board = Board.startingPosition();
        assertEquals(20, board.pseudoLegalMoves(WHITE).size());
        assertEquals(20, board.pseudoLegalMoves(BLACK).size());
    }

    @Test
    void onlyPawnsAndKnightsCanMoveAtTheStart() {
        Board board = Board.startingPosition();
        for (String square : new String[] {"a1", "c1", "d1", "e1", "f1", "h1", "a8", "c8", "d8", "e8", "f8", "h8"}) {
            assertTrue(board.pseudoLegalMovesFrom(Position.of(square)).isEmpty());
        }
        assertEquals(2, board.pseudoLegalMovesFrom(Position.of("b1")).size());
        assertEquals(2, board.pseudoLegalMovesFrom(Position.of("g8")).size());
        assertEquals(2, board.pseudoLegalMovesFrom(Position.of("e2")).size());
    }

    @Test
    void whiteHasThirtyMovesAfterAdvancingTheKingPawn() {
        Board board = Board.startingPosition();
        play(board, "e2", "e4");
        // 15 pawn moves + 5 knight moves + 5 bishop moves + 4 queen moves + 1 king move
        assertEquals(30, board.pseudoLegalMoves(WHITE).size());
        assertEquals(20, board.pseudoLegalMoves(BLACK).size());
    }

    @Test
    void makeMoveMovesThePiece() {
        Board board = Board.startingPosition();
        play(board, "g1", "f3");
        assertTrue(board.isEmpty(Position.of("g1")));
        assertEquals(new Knight(WHITE), board.getPiece(Position.of("f3")));
    }

    @Test
    void makeMoveCapturesThePieceOnTheDestination() {
        Board board = new Board();
        board.setPiece(Position.of("a1"), new Rook(WHITE));
        board.setPiece(Position.of("a8"), new Rook(BLACK));

        play(board, "a1", "a8");

        assertTrue(board.isEmpty(Position.of("a1")));
        assertEquals(new Rook(WHITE), board.getPiece(Position.of("a8")));
        assertTrue(board.pseudoLegalMoves(BLACK).isEmpty());  // Black has nothing left
    }

    @Test
    void makeMoveRejectsAPieceThatIsNotOnItsStartingSquare() {
        Board board = Board.startingPosition();
        Move fromEmptySquare = new Move(Position.of("e4"), Position.of("e5"), new Pawn(WHITE), null);
        Move wrongPiece = new Move(Position.of("e2"), Position.of("e4"), new Queen(WHITE), null);

        assertThrows(IllegalArgumentException.class, () -> board.makeMove(fromEmptySquare));
        assertThrows(IllegalArgumentException.class, () -> board.makeMove(wrongPiece));
        assertEquals(new Pawn(WHITE), board.getPiece(Position.of("e2")));  // board unchanged
    }

    @Test
    void copyCanBeChangedWithoutAffectingTheOriginal() {
        Board original = Board.startingPosition();
        Board copy = new Board(original);

        play(copy, "e2", "e4");

        assertFalse(original.isEmpty(Position.of("e2")));
        assertNull(original.getPiece(Position.of("e4")));
        assertTrue(copy.isEmpty(Position.of("e2")));
        assertEquals(new Pawn(WHITE), copy.getPiece(Position.of("e4")));
    }

    @Test
    void printsTheStartingPositionFromWhitesSide() {
        String expected =
                  "  +------------------------+\n"
                + "8 | r  n  b  q  k  b  n  r |\n"
                + "7 | p  p  p  p  p  p  p  p |\n"
                + "6 | .  .  .  .  .  .  .  . |\n"
                + "5 | .  .  .  .  .  .  .  . |\n"
                + "4 | .  .  .  .  .  .  .  . |\n"
                + "3 | .  .  .  .  .  .  .  . |\n"
                + "2 | P  P  P  P  P  P  P  P |\n"
                + "1 | R  N  B  Q  K  B  N  R |\n"
                + "  +------------------------+\n"
                + "    a  b  c  d  e  f  g  h\n";
        assertEquals(expected, Board.startingPosition().toString());
    }
}
