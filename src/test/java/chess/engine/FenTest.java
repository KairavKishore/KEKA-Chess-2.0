package chess.engine;

import static chess.engine.CastlingSide.KING_SIDE;
import static chess.engine.CastlingSide.QUEEN_SIDE;
import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FenTest {

    private static void play(Game game, String from, String to) {
        for (Move move : game.legalMovesFrom(Position.of(from))) {
            if (move.getTo().equals(Position.of(to))) {
                game.makeMove(move);
                return;
            }
        }
        throw new AssertionError("No legal move from " + from + " to " + to);
    }

    private static void assertInvalid(String fen) {
        assertThrows(IllegalArgumentException.class, () -> Game.fromFen(fen), fen);
    }

    // ------------------------------------------------------------- writing

    @Test
    void newGameIsTheStandardStartingFen() {
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", new Game().toFen());
        assertEquals(Fen.STARTING_POSITION, new Game().toFen());
    }

    @Test
    void fenFollowsTheGameMoveByMove() {
        // The example game from the FEN article on Wikipedia.
        Game game = new Game();

        play(game, "e2", "e4");
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", game.toFen());

        play(game, "c7", "c5");
        assertEquals("rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2", game.toFen());

        play(game, "g1", "f3");
        assertEquals("rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2", game.toFen());
    }

    @Test
    void castlingRightsDisappearFromTheFenAsTheyAreLost() {
        Game game = Game.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");

        play(game, "h1", "g1");
        assertEquals("r3k2r/8/8/8/8/8/8/R3K1R1 b Qkq - 1 1", game.toFen());

        play(game, "e8", "d8");
        assertEquals("r2k3r/8/8/8/8/8/8/R3K1R1 w Q - 2 2", game.toFen());

        play(game, "a1", "b1");
        assertEquals("r2k3r/8/8/8/8/8/8/1R2K1R1 b - - 3 2", game.toFen());
    }

    // ------------------------------------------------------------- reading

    @Test
    void readsEveryField() {
        Game game = Game.fromFen("r3k2r/8/8/3pP3/8/8/8/4K2R w Kq d6 4 31");
        Board board = game.getBoard();

        assertEquals(new Rook(BLACK), board.getPiece(Position.of("a8")));
        assertEquals(new King(BLACK), board.getPiece(Position.of("e8")));
        assertEquals(new Pawn(BLACK), board.getPiece(Position.of("d5")));
        assertEquals(new Pawn(WHITE), board.getPiece(Position.of("e5")));
        assertEquals(new King(WHITE), board.getPiece(Position.of("e1")));
        assertEquals(new Rook(WHITE), board.getPiece(Position.of("h1")));
        assertNull(board.getPiece(Position.of("a1")));

        assertEquals(WHITE, game.getTurn());
        assertTrue(board.hasCastlingRight(WHITE, KING_SIDE));
        assertFalse(board.hasCastlingRight(WHITE, QUEEN_SIDE));
        assertFalse(board.hasCastlingRight(BLACK, KING_SIDE));
        assertTrue(board.hasCastlingRight(BLACK, QUEEN_SIDE));
        assertEquals(Position.of("d6"), board.getEnPassantTarget());
        assertEquals(4, board.getHalfmoveClock());
        assertEquals(31, game.getFullmoveNumber());
    }

    @Test
    void loadedPositionPlaysByTheFieldsItWasGiven() {
        Game game = Game.fromFen("r3k2r/8/8/3pP3/8/8/8/4K2R w Kq d6 4 31");

        play(game, "e5", "d6");  // en passant, allowed by the "d6" field

        assertTrue(game.getBoard().isEmpty(Position.of("d5")));
        assertEquals("r3k2r/8/3P4/8/8/8/8/4K2R b Kq - 0 31", game.toFen());
    }

    @Test
    void blackToMoveIsRead() {
        Game game = Game.fromFen("4k3/8/8/8/8/8/8/4K2R b K - 0 1");
        assertEquals(BLACK, game.getTurn());
        assertEquals(5, game.legalMoves().size());
    }

    @Test
    void enPassantSquareIsReadForEitherSide() {
        Game game = Game.fromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        assertEquals(Position.of("e3"), game.getBoard().getEnPassantTarget());
    }

    @Test
    void theTwoCountersMayBeLeftOut() {
        Game game = Game.fromFen("4k3/8/8/8/8/8/8/4K2R w K -");
        assertEquals(0, game.getBoard().getHalfmoveClock());
        assertEquals(1, game.getFullmoveNumber());
        assertEquals("4k3/8/8/8/8/8/8/4K2R w K - 0 1", game.toFen());
    }

    @Test
    void extraSpacesAreIgnored() {
        assertEquals(Fen.STARTING_POSITION,
                Game.fromFen("  rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR  w KQkq -  0 1 ").toFen());
    }

    @Test
    void everyPositionSurvivesARoundTrip() {
        String[] positions = {
            Fen.STARTING_POSITION,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 b - - 12 40",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            "8/8/8/8/8/8/8/K6k w - - 99 120",
        };
        for (String fen : positions) {
            assertEquals(fen, Game.fromFen(fen).toFen());
        }
    }

    // -------------------------------------------------------------- errors

    @Test
    void rejectsTheWrongNumberOfFields() {
        assertInvalid(null);
        assertInvalid("");
        assertInvalid("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");
        assertInvalid("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0");
        assertInvalid("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 extra");
    }

    @Test
    void rejectsABadBoard() {
        assertInvalid("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1");            // 7 ranks
        assertInvalid("rnbqkbnr/pppppppp/8/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");  // 9 ranks
        assertInvalid("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPP/RNBQKBNR w KQkq - 0 1");     // rank 2 too short
        assertInvalid("rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");    // rank 6 too long
        assertInvalid("rnbqkbnr/ppppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");   // nine pawns
        assertInvalid("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKXNR w KQkq - 0 1");    // no such piece
        assertInvalid("rnbqkbnr/pppppppp/08/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");   // 0 is not a count
    }

    @Test
    void rejectsBadValuesInTheOtherFields() {
        assertInvalid("4k3/8/8/8/8/8/8/4K3 x - - 0 1");    // side to move
        assertInvalid("4k3/8/8/8/8/8/8/4K3 W - - 0 1");
        assertInvalid("4k3/8/8/8/8/8/8/4K3 w KX - 0 1");   // castling
        assertInvalid("4k3/8/8/8/8/8/8/4K3 w - e9 0 1");   // not a square
        assertInvalid("4k3/8/8/8/8/8/8/4K3 w - e4 0 1");   // not a square a pawn can skip
        assertInvalid("4k3/8/8/8/8/8/8/4K3 w - - -1 1");   // clocks
        assertInvalid("4k3/8/8/8/8/8/8/4K3 w - - x 1");
        assertInvalid("4k3/8/8/8/8/8/8/4K3 w - - 0 0");
    }

    @Test
    void lettersMapToPieces() {
        assertEquals(new Knight(WHITE), Piece.fromSymbol('N'));
        assertEquals(new Knight(BLACK), Piece.fromSymbol('n'));
        assertEquals(new King(WHITE), Piece.fromSymbol('K'));
        assertEquals(new Pawn(BLACK), Piece.fromSymbol('p'));
        for (PieceType type : PieceType.values()) {
            assertEquals(type, PieceType.fromSymbol(type.getSymbol()));
        }
        assertThrows(IllegalArgumentException.class, () -> Piece.fromSymbol('x'));
    }
}
