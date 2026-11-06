package chess.engine;

import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameTest {

    /** Plays the move between two squares, the way a user interface would. */
    private static void play(Game game, String from, String to) {
        for (Move move : game.legalMovesFrom(Position.of(from))) {
            if (move.getTo().equals(Position.of(to))) {
                game.makeMove(move);
                return;
            }
        }
        throw new AssertionError("No legal move from " + from + " to " + to);
    }

    @Test
    void whiteMovesFirstAndTheTurnAlternates() {
        Game game = new Game();
        assertEquals(WHITE, game.getTurn());

        play(game, "e2", "e4");
        assertEquals(BLACK, game.getTurn());

        play(game, "e7", "e5");
        assertEquals(WHITE, game.getTurn());
    }

    @Test
    void newGameOffersTwentyMoves() {
        assertEquals(20, new Game().legalMoves().size());
    }

    @Test
    void offersMovesOnlyForTheSideToMove() {
        Game game = new Game();
        assertEquals(2, game.legalMovesFrom(Position.of("e2")).size());
        assertTrue(game.legalMovesFrom(Position.of("e7")).isEmpty());  // Black's pawn, White to move
        assertTrue(game.legalMovesFrom(Position.of("e4")).isEmpty());  // empty square

        play(game, "e2", "e4");
        assertEquals(2, game.legalMovesFrom(Position.of("e7")).size());
        assertTrue(game.legalMovesFrom(Position.of("d2")).isEmpty());  // White's pawn, Black to move
    }

    @Test
    void rejectsAMoveThePieceCannotMake() {
        Game game = new Game();
        Move rookThroughItsOwnPawn = new Move(Position.of("a1"), Position.of("a5"), new Rook(WHITE), null);

        assertThrows(IllegalArgumentException.class, () -> game.makeMove(rookThroughItsOwnPawn));

        assertEquals(WHITE, game.getTurn());  // nothing changed
        assertEquals(new Rook(WHITE), game.getBoard().getPiece(Position.of("a1")));
        assertTrue(game.getBoard().isEmpty(Position.of("a5")));
    }

    @Test
    void rejectsAMoveMadeOutOfTurn() {
        Game game = new Game();
        Move blackMovesFirst = new Move(Position.of("e7"), Position.of("e5"), new Pawn(BLACK), null);

        assertThrows(IllegalArgumentException.class, () -> game.makeMove(blackMovesFirst));

        assertEquals(WHITE, game.getTurn());
        assertEquals(new Pawn(BLACK), game.getBoard().getPiece(Position.of("e7")));
    }

    @Test
    void canStartFromAnyPositionWithEitherSideToMove() {
        Board board = new Board();
        board.setPiece(Position.of("e1"), new King(WHITE));
        board.setPiece(Position.of("e8"), new King(BLACK));
        board.setPiece(Position.of("a8"), new Rook(BLACK));

        Game game = new Game(board, BLACK);

        assertEquals(BLACK, game.getTurn());
        assertEquals(5 + 10, game.legalMoves().size());  // king: 5 squares, rook: 7 down the file + 3 along the rank
    }

    @Test
    void newGameIsInProgress() {
        Game game = new Game();
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertFalse(game.isInCheck());
        assertFalse(game.getStatus().isGameOver());
    }

    @Test
    void reportsCheckAndOffersOnlyMovesThatAnswerIt() {
        Game game = new Game();
        play(game, "e2", "e4");
        play(game, "d7", "d5");
        play(game, "f1", "b5");  // bishop checks along the diagonal opened by d7-d5

        assertEquals(GameStatus.CHECK, game.getStatus());
        assertTrue(game.isInCheck());
        // Block on c6 (pawn or knight) or on d7 (knight, bishop or queen).
        assertEquals(5, game.legalMoves().size());
        assertTrue(game.legalMovesFrom(Position.of("a7")).isEmpty());
    }

    @Test
    void rejectsAMoveThatLeavesTheMoversKingInCheck() {
        Game game = new Game();
        play(game, "e2", "e4");
        play(game, "d7", "d5");
        play(game, "f1", "b5");
        Move ignoresTheCheck = new Move(Position.of("a7"), Position.of("a6"), new Pawn(BLACK), null);

        assertThrows(IllegalArgumentException.class, () -> game.makeMove(ignoresTheCheck));
        assertEquals(BLACK, game.getTurn());
    }

    @Test
    void foolsMateIsCheckmate() {
        Game game = new Game();
        play(game, "f2", "f3");
        play(game, "e7", "e5");
        play(game, "g2", "g4");
        play(game, "d8", "h4");

        assertEquals(GameStatus.CHECKMATE, game.getStatus());
        assertTrue(game.getStatus().isGameOver());
        assertTrue(game.isInCheck());
        assertEquals(WHITE, game.getTurn());  // the side to move is the one that lost
        assertTrue(game.legalMoves().isEmpty());
    }

    @Test
    void noMovesWithoutCheckIsStalemate() {
        Board board = new Board();
        board.setPiece(Position.of("h8"), new King(BLACK));
        board.setPiece(Position.of("f7"), new King(WHITE));
        board.setPiece(Position.of("g6"), new Queen(WHITE));

        Game game = new Game(board, BLACK);

        assertEquals(GameStatus.STALEMATE, game.getStatus());
        assertFalse(game.isInCheck());
        assertTrue(game.legalMoves().isEmpty());
    }

    @Test
    void backRankMate() {
        Game game = Game.fromFen("6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1");
        play(game, "a1", "a8");
        assertEquals(GameStatus.CHECKMATE, game.getStatus());
    }

    @Test
    void smotheredMateByAKnightThatNothingCanBlock() {
        Game game = Game.fromFen("6rk/6pp/8/6N1/8/8/8/4K3 w - - 0 1");
        play(game, "g5", "f7");
        assertEquals(GameStatus.CHECKMATE, game.getStatus());
    }

    @Test
    void checkThatCanBeBlockedOrCapturedIsNotMate() {
        // The king is boxed in by its own pieces, so each time there is exactly one answer.
        Game blockable = Game.fromFen("6k1/5ppp/8/8/8/8/3PPR2/4K2r w - - 0 1");
        assertEquals(GameStatus.CHECK, blockable.getStatus());
        assertEquals(1, blockable.legalMoves().size());
        assertEquals("f2f1", blockable.legalMoves().get(0).toString());

        Game capturable = Game.fromFen("R5k1/5ppp/8/8/8/8/4K3/r7 b - - 0 1");
        assertEquals(GameStatus.CHECK, capturable.getStatus());
        assertEquals(1, capturable.legalMoves().size());
        assertEquals("a1a8", capturable.legalMoves().get(0).toString());
    }

    @Test
    void doubleCheckCanOnlyBeAnsweredByMovingTheKing() {
        // The rook on e1 and the bishop on b5 both give check. Black's rook
        // could take the bishop and Black's queen could block either line,
        // but neither deals with the other checker.
        Game game = Game.fromFen("3qk3/8/8/rB6/8/8/8/4R2K b - - 0 1");
        assertTrue(game.getBoard().isSquareAttacked(Position.of("b5"), BLACK));
        assertEquals(GameStatus.CHECK, game.getStatus());
        for (Move move : game.legalMoves()) {
            assertEquals(PieceType.KING, move.getPiece().getType(), move.toString());
        }
        assertFalse(game.legalMoves().isEmpty());
    }

    @Test
    void promotionCanDeliverCheckmate() {
        Game game = Game.fromFen("7k/4P1pp/8/8/8/8/8/4K3 w - - 0 1");
        Move toQueen = game.legalMovesFrom(Position.of("e7")).get(0);
        assertEquals(PieceType.QUEEN, toQueen.getPromotion());

        game.makeMove(toQueen);

        assertEquals(GameStatus.CHECKMATE, game.getStatus());
    }

    @Test
    void underpromotionAvoidsTheStalemateAQueenWouldCause() {
        String position = "8/5P1k/8/6K1/8/8/8/8 w - - 0 1";
        Game queening = Game.fromFen(position);
        queening.makeMove(queening.legalMovesFrom(Position.of("f7")).get(0));
        assertEquals(GameStatus.STALEMATE, queening.getStatus());

        Game rooking = Game.fromFen(position);
        rooking.makeMove(rooking.legalMovesFrom(Position.of("f7")).get(1));
        assertEquals(PieceType.ROOK, rooking.getLastMove().getPromotion());
        assertEquals(GameStatus.IN_PROGRESS, rooking.getStatus());
    }

    @Test
    void playsAWholeFamousGameThroughToCheckmate() {
        // Morphy v Duke of Brunswick and Count Isouard, Paris 1858 (the "Opera Game").
        // It has captures, checks, pins, queenside castling and a queen sacrifice.
        String[] moves = {
            "e2e4", "e7e5", "g1f3", "d7d6", "d2d4", "c8g4", "d4e5", "g4f3", "d1f3", "d6e5",
            "f1c4", "g8f6", "f3b3", "d8e7", "b1c3", "c7c6", "c1g5", "b7b5", "c3b5", "c6b5",
            "c4b5", "b8d7", "e1c1", "a8d8", "d1d7", "d8d7", "h1d1", "e7e6", "b5d7", "f6d7",
            "b3b8", "d7b8", "d1d8",
        };
        Game game = new Game();
        for (String move : moves) {
            assertFalse(game.getStatus().isGameOver(), "game ended before " + move);
            play(game, move.substring(0, 2), move.substring(2));
        }

        assertEquals(GameStatus.CHECKMATE, game.getStatus());
        assertEquals(BLACK, game.getTurn());
        assertEquals("1n1Rkb1r/p4ppp/4q3/4p1B1/4P3/8/PPP2PPP/2K5 b k - 1 17", game.toFen());
        assertEquals(moves.length, game.getMoveHistory().size());

        while (game.canUndo()) {
            game.undo();
        }
        assertEquals(Fen.STARTING_POSITION, game.toFen());
    }

    @Test
    void needsABoardAndASideToMove() {
        assertThrows(NullPointerException.class, () -> new Game(null, WHITE));
        assertThrows(NullPointerException.class, () -> new Game(new Board(), null));
    }

    @Test
    void castlesInARealGame() {
        Game game = new Game();
        play(game, "e2", "e4");
        play(game, "e7", "e5");
        play(game, "g1", "f3");
        play(game, "b8", "c6");
        play(game, "f1", "c4");
        play(game, "f8", "c5");
        play(game, "e1", "g1");

        Board board = game.getBoard();
        assertEquals(new King(WHITE), board.getPiece(Position.of("g1")));
        assertEquals(new Rook(WHITE), board.getPiece(Position.of("f1")));
        assertTrue(board.isEmpty(Position.of("h1")));
        assertFalse(board.hasCastlingRight(WHITE, CastlingSide.QUEEN_SIDE));
        assertTrue(board.hasCastlingRight(BLACK, CastlingSide.KING_SIDE));
        assertEquals(BLACK, game.getTurn());
    }

    @Test
    void capturesEnPassantInARealGame() {
        Game game = new Game();
        play(game, "e2", "e4");
        play(game, "a7", "a6");
        play(game, "e4", "e5");
        play(game, "d7", "d5");
        play(game, "e5", "d6");

        Board board = game.getBoard();
        assertEquals(new Pawn(WHITE), board.getPiece(Position.of("d6")));
        assertTrue(board.isEmpty(Position.of("d5")));
    }

    @Test
    void playsAnOpeningWithACapture() {
        Game game = new Game();
        play(game, "e2", "e4");
        play(game, "d7", "d5");
        play(game, "e4", "d5");  // pawn takes pawn
        play(game, "d8", "d5");  // queen takes back

        Board board = game.getBoard();
        assertEquals(new Queen(BLACK), board.getPiece(Position.of("d5")));
        assertTrue(board.isEmpty(Position.of("d8")));
        assertTrue(board.isEmpty(Position.of("e4")));
        assertTrue(board.isEmpty(Position.of("d7")));
        assertEquals(WHITE, game.getTurn());
    }
}
