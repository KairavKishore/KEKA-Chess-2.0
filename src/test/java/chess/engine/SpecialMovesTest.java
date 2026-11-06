package chess.engine;

import static chess.engine.CastlingSide.KING_SIDE;
import static chess.engine.CastlingSide.QUEEN_SIDE;
import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Castling, en passant and promotion. */
class SpecialMovesTest {

    private final Board board = new Board();

    private void place(String square, Piece piece) {
        board.setPiece(Position.of(square), piece);
    }

    private Piece pieceOn(String square) {
        return board.getPiece(Position.of(square));
    }

    /** The legal moves between two squares: none, one, or four for a promotion. */
    private List<Move> movesBetween(String from, String to) {
        List<Move> moves = new ArrayList<>();
        for (Move move : board.legalMovesFrom(Position.of(from))) {
            if (move.getTo().equals(Position.of(to))) {
                moves.add(move);
            }
        }
        return moves;
    }

    private boolean canMove(String from, String to) {
        return !movesBetween(from, to).isEmpty();
    }

    private Move play(String from, String to) {
        List<Move> moves = movesBetween(from, to);
        if (moves.isEmpty()) {
            throw new AssertionError("No legal move from " + from + " to " + to);
        }
        board.makeMove(moves.get(0));
        return moves.get(0);
    }

    // ------------------------------------------------------------ castling

    /** White king and both rooks at home with full castling rights; Black's king out of the way. */
    private void placeWhiteReadyToCastle() {
        place("e1", new King(WHITE));
        place("a1", new Rook(WHITE));
        place("h1", new Rook(WHITE));
        place("g7", new King(BLACK));
        board.setCastlingRight(WHITE, KING_SIDE, true);
        board.setCastlingRight(WHITE, QUEEN_SIDE, true);
    }

    @Test
    void startingPositionHasAllFourCastlingRightsAndAnEmptyBoardHasNone() {
        Board start = Board.startingPosition();
        for (PieceColor color : PieceColor.values()) {
            for (CastlingSide side : CastlingSide.values()) {
                assertTrue(start.hasCastlingRight(color, side));
                assertFalse(board.hasCastlingRight(color, side));
            }
        }
    }

    @Test
    void castlingKingSideMovesTheKingTwoSquaresAndTheRookNextToIt() {
        placeWhiteReadyToCastle();

        Move castle = play("e1", "g1");

        assertTrue(castle.isCastling());
        assertEquals(new King(WHITE), pieceOn("g1"));
        assertEquals(new Rook(WHITE), pieceOn("f1"));
        assertNull(pieceOn("e1"));
        assertNull(pieceOn("h1"));
        assertEquals(new Rook(WHITE), pieceOn("a1"));  // the other rook stays put
    }

    @Test
    void castlingQueenSideMovesTheKingTwoSquaresAndTheRookNextToIt() {
        placeWhiteReadyToCastle();

        play("e1", "c1");

        assertEquals(new King(WHITE), pieceOn("c1"));
        assertEquals(new Rook(WHITE), pieceOn("d1"));
        assertNull(pieceOn("a1"));
        assertNull(pieceOn("b1"));
        assertNull(pieceOn("e1"));
    }

    @Test
    void blackCastlesOnItsOwnBackRank() {
        place("e8", new King(BLACK));
        place("h8", new Rook(BLACK));
        place("e1", new King(WHITE));
        board.setCastlingRight(BLACK, KING_SIDE, true);

        play("e8", "g8");

        assertEquals(new King(BLACK), pieceOn("g8"));
        assertEquals(new Rook(BLACK), pieceOn("f8"));
        assertFalse(canMove("g8", "c8"));
    }

    @Test
    void anOrdinaryKingMoveIsNotCastling() {
        placeWhiteReadyToCastle();
        assertFalse(play("e1", "f1").isCastling());
        assertEquals(new Rook(WHITE), pieceOn("h1"));
    }

    @Test
    void cannotCastleWithoutTheRight() {
        placeWhiteReadyToCastle();
        board.setCastlingRight(WHITE, KING_SIDE, false);
        assertFalse(canMove("e1", "g1"));
        assertTrue(canMove("e1", "c1"));
    }

    @Test
    void cannotCastleWithARookThatIsNotThere() {
        // The right can only be set without its rook by hand, but castling must not invent one.
        placeWhiteReadyToCastle();
        board.setPiece(Position.of("h1"), null);
        board.setPiece(Position.of("a1"), new Bishop(WHITE));
        assertFalse(canMove("e1", "g1"));
        assertFalse(canMove("e1", "c1"));
    }

    @Test
    void cannotCastleWithAKingThatIsNotOnItsHomeSquare() {
        placeWhiteReadyToCastle();
        board.setPiece(Position.of("e1"), null);
        place("d1", new King(WHITE));
        assertFalse(canMove("d1", "f1"));
        assertFalse(canMove("d1", "b1"));
    }

    @Test
    void cannotCastleThroughAPiece() {
        placeWhiteReadyToCastle();
        place("g1", new Knight(WHITE));
        place("b1", new Knight(BLACK));  // even next to the rook, where the king never goes
        assertFalse(canMove("e1", "g1"));
        assertFalse(canMove("e1", "c1"));
    }

    @Test
    void cannotCastleOutOfCheck() {
        placeWhiteReadyToCastle();
        place("e8", new Rook(BLACK));
        assertFalse(canMove("e1", "g1"));
        assertFalse(canMove("e1", "c1"));
    }

    @Test
    void cannotCastleThroughAnAttackedSquare() {
        placeWhiteReadyToCastle();
        place("f8", new Rook(BLACK));  // attacks f1, which the king would cross
        assertFalse(canMove("e1", "g1"));
        assertTrue(canMove("e1", "c1"));
    }

    @Test
    void cannotCastleOntoAnAttackedSquare() {
        placeWhiteReadyToCastle();
        place("c8", new Rook(BLACK));  // attacks c1, where the king would land
        assertFalse(canMove("e1", "c1"));
        assertTrue(canMove("e1", "g1"));
    }

    @Test
    void canCastleWhenOnlyTheRookPassesAnAttackedSquare() {
        placeWhiteReadyToCastle();
        place("b8", new Rook(BLACK));  // attacks b1: the rook crosses it, the king does not
        place("h8", new Rook(BLACK));  // attacks the h1 rook itself
        assertTrue(canMove("e1", "c1"));
        assertTrue(canMove("e1", "g1"));
    }

    @Test
    void movingTheKingLosesBothRightsEvenIfItComesBack() {
        placeWhiteReadyToCastle();
        play("e1", "e2");
        play("e2", "e1");

        assertFalse(board.hasCastlingRight(WHITE, KING_SIDE));
        assertFalse(board.hasCastlingRight(WHITE, QUEEN_SIDE));
        assertFalse(canMove("e1", "g1"));
        assertFalse(canMove("e1", "c1"));
    }

    @Test
    void movingARookLosesOnlyThatSidesRight() {
        placeWhiteReadyToCastle();
        play("h1", "h5");
        play("h5", "h1");

        assertFalse(board.hasCastlingRight(WHITE, KING_SIDE));
        assertTrue(board.hasCastlingRight(WHITE, QUEEN_SIDE));
        assertFalse(canMove("e1", "g1"));
        assertTrue(canMove("e1", "c1"));
    }

    @Test
    void losingARookToACaptureLosesThatSidesRight() {
        placeWhiteReadyToCastle();
        place("h8", new Rook(BLACK));
        play("h8", "h1");
        place("h1", new Rook(WHITE));  // even with a new rook on the square

        assertFalse(board.hasCastlingRight(WHITE, KING_SIDE));
        assertTrue(board.hasCastlingRight(WHITE, QUEEN_SIDE));
        assertFalse(canMove("e1", "g1"));
    }

    @Test
    void copiedBoardKeepsItsOwnCastlingRights() {
        placeWhiteReadyToCastle();
        Board copy = new Board(board);

        play("e1", "e2");

        assertTrue(copy.hasCastlingRight(WHITE, KING_SIDE));
        assertFalse(board.hasCastlingRight(WHITE, KING_SIDE));
    }

    // ---------------------------------------------------------- en passant

    @Test
    void doubleStepLeavesAnEnPassantTargetForOneMoveOnly() {
        place("e2", new Pawn(WHITE));
        place("a7", new Pawn(BLACK));
        assertNull(board.getEnPassantTarget());

        play("e2", "e4");
        assertEquals(Position.of("e3"), board.getEnPassantTarget());

        play("a7", "a6");  // a single step sets no target and clears the old one
        assertNull(board.getEnPassantTarget());
    }

    @Test
    void pawnCapturesEnPassantOntoTheSkippedSquare() {
        place("e5", new Pawn(WHITE));
        place("d7", new Pawn(BLACK));
        play("d7", "d5");

        Move capture = play("e5", "d6");

        assertTrue(capture.isEnPassant());
        assertTrue(capture.isCapture());
        assertEquals(new Pawn(BLACK), capture.getCaptured());
        assertEquals(new Pawn(WHITE), pieceOn("d6"));
        assertNull(pieceOn("d5"));  // the captured pawn was here, not on d6
        assertNull(pieceOn("e5"));
    }

    @Test
    void blackCapturesEnPassantToo() {
        place("d4", new Pawn(BLACK));
        place("c2", new Pawn(WHITE));
        play("c2", "c4");

        play("d4", "c3");

        assertEquals(new Pawn(BLACK), pieceOn("c3"));
        assertNull(pieceOn("c4"));
    }

    @Test
    void enPassantIsOnlyAvailableOnTheVeryNextMove() {
        place("e5", new Pawn(WHITE));
        place("d7", new Pawn(BLACK));
        place("h2", new Pawn(WHITE));
        place("h7", new Pawn(BLACK));
        play("d7", "d5");
        assertTrue(canMove("e5", "d6"));

        play("h2", "h3");
        play("h7", "h6");

        assertFalse(canMove("e5", "d6"));
    }

    @Test
    void noEnPassantAgainstAPawnThatMovedOneSquareAtATime() {
        place("e5", new Pawn(WHITE));
        place("d6", new Pawn(BLACK));
        play("d6", "d5");
        assertFalse(canMove("e5", "d6"));
    }

    @Test
    void enPassantSquareWithNoPawnBesideItCapturesNothing() {
        // Only possible in a position set up by hand: the square says a pawn
        // just passed, but a knight is standing where that pawn would be.
        place("e5", new Pawn(WHITE));
        place("d5", new Knight(BLACK));
        board.setEnPassantTarget(Position.of("d6"));
        assertFalse(canMove("e5", "d6"));
    }

    @Test
    void ordinaryPawnCaptureIsNotEnPassant() {
        place("e4", new Pawn(WHITE));
        place("d5", new Pawn(BLACK));
        assertFalse(play("e4", "d5").isEnPassant());
    }

    @Test
    void enPassantIsIllegalWhenItUncoversTheKing() {
        // Taking en passant removes both pawns from the fifth rank at once,
        // leaving nothing between the rook and the king.
        place("a5", new King(WHITE));
        place("e5", new Pawn(WHITE));
        place("h5", new Rook(BLACK));
        place("d7", new Pawn(BLACK));
        place("h8", new King(BLACK));
        play("d7", "d5");

        assertFalse(canMove("e5", "d6"));
        assertTrue(canMove("e5", "e6"));
    }

    // ----------------------------------------------------------- promotion

    @Test
    void pawnReachingTheLastRankHasFourMovesToTheSameSquare() {
        place("e7", new Pawn(WHITE));

        List<Move> moves = movesBetween("e7", "e8");

        List<PieceType> choices = new ArrayList<>();
        for (Move move : moves) {
            assertTrue(move.isPromotion());
            choices.add(move.getPromotion());
        }
        assertEquals(List.of(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT), choices);
    }

    @Test
    void promotingReplacesThePawnWithTheChosenPiece() {
        place("e7", new Pawn(WHITE));
        Pawn pawn = new Pawn(WHITE);

        board.makeMove(new Move(Position.of("e7"), Position.of("e8"), pawn, null, PieceType.KNIGHT));

        assertEquals(new Knight(WHITE), pieceOn("e8"));
        assertNull(pieceOn("e7"));
    }

    @Test
    void pawnCanPromoteByCapturing() {
        place("e7", new Pawn(WHITE));
        place("e8", new Rook(BLACK));  // blocks the straight move
        place("d8", new Knight(BLACK));

        assertTrue(movesBetween("e7", "e8").isEmpty());
        List<Move> captures = movesBetween("e7", "d8");
        assertEquals(4, captures.size());
        assertEquals(new Knight(BLACK), captures.get(0).getCaptured());

        board.makeMove(captures.get(0));
        assertEquals(new Queen(WHITE), pieceOn("d8"));
    }

    @Test
    void blackPromotesOnTheFirstRank() {
        place("b2", new Pawn(BLACK));

        play("b2", "b1");

        assertEquals(new Queen(BLACK), pieceOn("b1"));
    }

    @Test
    void movesBeforeTheLastRankAreNotPromotions() {
        place("e6", new Pawn(WHITE));
        List<Move> moves = movesBetween("e6", "e7");
        assertEquals(1, moves.size());
        assertFalse(moves.get(0).isPromotion());
        assertNull(moves.get(0).getPromotion());
    }

    @Test
    void promotionShowsInTheMoveText() {
        Pawn pawn = new Pawn(WHITE);
        Move promotion = new Move(Position.of("e7"), Position.of("e8"), pawn, null, PieceType.QUEEN);
        assertEquals("e7e8q", promotion.toString());
        assertEquals("e7e8n",
                new Move(Position.of("e7"), Position.of("e8"), pawn, null, PieceType.KNIGHT).toString());
    }

    @Test
    void factoryBuildsEveryKindOfPiece() {
        for (PieceType type : PieceType.values()) {
            Piece piece = Piece.create(type, BLACK);
            assertEquals(type, piece.getType());
            assertEquals(BLACK, piece.getColor());
        }
        assertEquals(new Queen(WHITE), Piece.create(PieceType.QUEEN, WHITE));
    }
}
