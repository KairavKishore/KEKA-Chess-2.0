package chess.ai;

import static chess.ai.SearchStrategy.CHECKMATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.engine.Board;
import chess.engine.Fen;
import chess.engine.Game;
import chess.engine.GameStatus;
import chess.engine.Move;
import chess.engine.Pawn;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import chess.engine.Position;
import chess.engine.Queen;
import chess.engine.Rook;
import java.util.List;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;

/**
 * Tests that minimax and alpha-beta find the right moves, and that they
 * agree with each other. Most tests run against all three variants: plain
 * minimax, alpha-beta without move ordering, and alpha-beta with it.
 */
class SearchStrategyTest {

    private static final Evaluator EVALUATOR = new PieceSquareEvaluator();

    private static final String KIWIPETE =
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";

    private static final String[] POSITIONS = {
        Fen.STARTING_POSITION,
        KIWIPETE,
        "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
        "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
        "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
        "r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1",
        "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
    };

    private static MinimaxStrategy minimax(int depth) {
        return new MinimaxStrategy(depth, EVALUATOR);
    }

    private static AlphaBetaStrategy alphaBeta(int depth) {
        return new AlphaBetaStrategy(depth, EVALUATOR);
    }

    private static AlphaBetaStrategy unorderedAlphaBeta(int depth) {
        return new AlphaBetaStrategy(depth, EVALUATOR, false);
    }

    /** The three variants at one depth. */
    private static List<SearchStrategy> variants(int depth) {
        return List.of(minimax(depth), unorderedAlphaBeta(depth), alphaBeta(depth));
    }

    private static Move choose(SearchStrategy strategy, String fen) {
        Game game = Game.fromFen(fen);
        return strategy.chooseMove(game.getBoard(), game.getTurn());
    }

    // ------------------------------------------------------ finding moves

    @Test
    void takesAQueenLeftUndefended() {
        for (SearchStrategy strategy : variants(1)) {
            assertEquals("d1d8", choose(strategy, "3q2k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1").toString(),
                    strategy.getName());
        }
    }

    @Test
    void doesNotTakeAPawnThatIsDefended() {
        // Queen takes pawn wins 100 and loses 900 to the recapture. Depth 1
        // cannot see the recapture; depth 2 can.
        String fen = "6k1/2p3pp/3p4/8/8/8/3Q2PP/6K1 w - - 0 1";
        assertEquals("d2d6", choose(minimax(1), fen).toString());
        for (SearchStrategy strategy : variants(2)) {
            assertNotEquals("d2d6", choose(strategy, fen).toString(), strategy.getName());
        }
    }

    @Test
    void findsMateInOne() {
        for (int depth = 1; depth <= 3; depth++) {
            for (SearchStrategy strategy : variants(depth)) {
                Move move = choose(strategy, "6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1");
                assertEquals("a1a8", move.toString(), strategy.getName());
                assertEquals(CHECKMATE - 1, strategy.getLastScore(), strategy.getName());
            }
        }
    }

    @Test
    void findsMateInOneForBlack() {
        for (SearchStrategy strategy : variants(2)) {
            Move move = choose(strategy, "r3k3/8/8/8/8/8/5PPP/6K1 b - - 0 1");
            assertEquals("a8a1", move.toString(), strategy.getName());
            assertEquals(CHECKMATE - 1, strategy.getLastScore(), strategy.getName());
        }
    }

    @Test
    void findsMateInTwoAndPlaysItOut() {
        // King and rook against king. Checking at once lets the king out;
        // the mate needs a quiet king move first.
        String fen = "7k/8/5K2/8/8/8/8/R7 w - - 0 1";
        for (SearchStrategy strategy : variants(3)) {
            Game game = Game.fromFen(fen);
            game.makeMove(strategy.chooseMove(new Board(game.getBoard()), game.getTurn()));
            assertEquals(CHECKMATE - 3, strategy.getLastScore(), strategy.getName());

            game.makeMove(game.legalMoves().get(0));  // Black's reply is forced
            game.makeMove(strategy.chooseMove(new Board(game.getBoard()), game.getTurn()));

            assertEquals(GameStatus.CHECKMATE, game.getStatus(), strategy.getName());
        }
    }

    @Test
    void prefersTheQuickerMate() {
        // Mate in one is available, and so are slower ways to win.
        String fen = "6k1/5ppp/8/8/8/8/1Q6/R3K3 w - - 0 1";
        for (SearchStrategy strategy : variants(3)) {
            choose(strategy, fen);
            assertEquals(CHECKMATE - 1, strategy.getLastScore(), strategy.getName());
        }
    }

    @Test
    void seesThatItIsGoingToBeMated() {
        // White's only move is Kg1, and then the rook mates on a1.
        String fen = "6k1/8/8/8/8/r7/1r6/7K w - - 0 1";
        for (SearchStrategy strategy : variants(2)) {
            assertEquals("h1g1", choose(strategy, fen).toString(), strategy.getName());
            assertEquals(-(CHECKMATE - 2), strategy.getLastScore(), strategy.getName());
        }
    }

    @Test
    void stopsAMateThatIsThreatened() {
        // Black threatens Ra1 mate. White has several ways to give the king
        // an escape square, and must find one of them.
        String fen = "6k1/5ppp/8/8/8/8/r4PPP/6K1 w - - 0 1";
        for (SearchStrategy strategy : variants(3)) {
            Game game = Game.fromFen(fen);
            game.makeMove(strategy.chooseMove(new Board(game.getBoard()), game.getTurn()));
            assertTrue(strategy.getLastScore() > -CHECKMATE / 2, strategy.getName());

            for (Move reply : game.legalMoves()) {
                Board board = new Board(game.getBoard());
                board.makeMove(reply);
                boolean mated = board.isInCheck(PieceColor.WHITE) && board.legalMoves(PieceColor.WHITE).isEmpty();
                assertTrue(!mated, strategy.getName() + " allowed " + reply + " mate");
            }
        }
    }

    @Test
    void avoidsStalematingWhenItIsWinning() {
        // Qg6 would be stalemate (score 0); almost anything else keeps a won game.
        String fen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1";
        for (SearchStrategy strategy : variants(2)) {
            Move move = choose(strategy, fen);
            assertNotEquals("g5g6", move.toString(), strategy.getName());
            assertTrue(strategy.getLastScore() > 500, strategy.getName());
        }
    }

    @Test
    void doesNotCashInMaterialForADeadDraw() {
        // Taking the pawn leaves king and bishop against king, which nobody
        // can win. Keeping the pawn on scores higher than that draw.
        String fen = "8/8/8/4k3/8/8/1p6/B1K5 w - - 0 1";
        for (SearchStrategy strategy : variants(1)) {
            Move move = choose(strategy, fen);
            assertTrue(!move.isCapture(), strategy.getName() + " played " + move);
            assertTrue(strategy.getLastScore() > 0, strategy.getName());
        }
    }

    // ------------------------------------------- minimax against alpha-beta

    @Test
    void alphaBetaGivesTheSameScoreAsMinimax() {
        for (String fen : POSITIONS) {
            for (int depth = 1; depth <= 3; depth++) {
                MinimaxStrategy minimax = minimax(depth);
                AlphaBetaStrategy unordered = unorderedAlphaBeta(depth);
                AlphaBetaStrategy ordered = alphaBeta(depth);
                choose(minimax, fen);
                choose(unordered, fen);
                choose(ordered, fen);

                String where = "depth " + depth + " in " + fen;
                assertEquals(minimax.getLastScore(), unordered.getLastScore(), where);
                assertEquals(minimax.getLastScore(), ordered.getLastScore(), where);
            }
        }
    }

    @Test
    void alphaBetaWithoutOrderingChoosesTheVerySameMoveAsMinimax() {
        // Both try moves in the same order and keep the first best one.
        for (String fen : POSITIONS) {
            for (int depth = 1; depth <= 3; depth++) {
                assertEquals(choose(minimax(depth), fen), choose(unorderedAlphaBeta(depth), fen),
                        "depth " + depth + " in " + fen);
            }
        }
    }

    @Test
    void orderedAlphaBetaChoosesAMoveThatMinimaxRatesAsBest() {
        // With ordering the move may differ from minimax's, but only between
        // moves of equal score. Check by searching the position after it.
        for (String fen : POSITIONS) {
            int depth = 3;
            MinimaxStrategy minimax = minimax(depth);
            choose(minimax, fen);

            Game game = Game.fromFen(fen);
            game.makeMove(choose(alphaBeta(depth), fen));
            if (game.getStatus().isGameOver()) {
                continue;
            }
            MinimaxStrategy reply = minimax(depth - 1);
            reply.chooseMove(game.getBoard(), game.getTurn());
            assertEquals(minimax.getLastScore(), -reply.getLastScore(), fen);
        }
    }

    @Test
    void alphaBetaLooksAtFewerPositionsAndOrderingHelpsFurther() {
        long minimaxTotal = 0;
        long unorderedTotal = 0;
        long orderedTotal = 0;
        for (String fen : POSITIONS) {
            MinimaxStrategy minimax = minimax(3);
            AlphaBetaStrategy unordered = unorderedAlphaBeta(3);
            AlphaBetaStrategy ordered = alphaBeta(3);
            choose(minimax, fen);
            choose(unordered, fen);
            choose(ordered, fen);

            assertTrue(unordered.getNodesSearched() <= minimax.getNodesSearched(), fen);
            assertTrue(ordered.getNodesSearched() <= minimax.getNodesSearched(), fen);
            minimaxTotal += minimax.getNodesSearched();
            unorderedTotal += unordered.getNodesSearched();
            orderedTotal += ordered.getNodesSearched();
        }
        assertTrue(unorderedTotal * 4 < minimaxTotal,
                "pruning alone: " + unorderedTotal + " of " + minimaxTotal);
        assertTrue(orderedTotal * 2 < unorderedTotal,
                "with ordering: " + orderedTotal + " of " + unorderedTotal);
    }

    @Test
    void minimaxAtDepthOneLooksAtEachMoveOnce() {
        MinimaxStrategy strategy = minimax(1);
        choose(strategy, Fen.STARTING_POSITION);
        assertEquals(1 + 20, strategy.getNodesSearched());  // the root and its 20 moves

        MinimaxStrategy deeper = minimax(2);
        choose(deeper, Fen.STARTING_POSITION);
        assertEquals(1 + 20 + 400, deeper.getNodesSearched());
    }

    // ------------------------------------------------------------ symmetry

    @Test
    void blackFindsTheMirrorImageOfWhitesScore() {
        for (String fen : POSITIONS) {
            String swapped = EvaluatorTest.swapColours(fen);
            AlphaBetaStrategy first = alphaBeta(3);
            AlphaBetaStrategy second = alphaBeta(3);
            choose(first, fen);
            choose(second, swapped);
            assertEquals(first.getLastScore(), second.getLastScore(), fen);
        }
    }

    // ----------------------------------------------------------- behaviour

    @Test
    void leavesTheBoardExactlyAsItFoundIt() {
        for (SearchStrategy strategy : variants(3)) {
            Game game = Game.fromFen(KIWIPETE);
            strategy.chooseMove(game.getBoard(), game.getTurn());
            assertEquals(KIWIPETE, game.toFen(), strategy.getName());
        }
    }

    @Test
    void alwaysReturnsALegalMove() {
        for (String fen : POSITIONS) {
            for (SearchStrategy strategy : variants(2)) {
                Game game = Game.fromFen(fen);
                Move move = strategy.chooseMove(new Board(game.getBoard()), game.getTurn());
                assertTrue(game.legalMoves().contains(move), strategy.getName() + " in " + fen);
            }
        }
    }

    @Test
    void sameStrategyCanBeUsedForMoveAfterMove() {
        AlphaBetaStrategy strategy = alphaBeta(2);
        Game game = new Game();
        for (int i = 0; i < 12; i++) {
            game.makeMove(strategy.chooseMove(new Board(game.getBoard()), game.getTurn()));
            assertTrue(strategy.getNodesSearched() > 0);
        }
        assertEquals(12, game.getMoveHistory().size());
    }

    @Test
    void refusesWhenThereIsNoLegalMove() {
        for (SearchStrategy strategy : variants(2)) {
            assertThrows(IllegalStateException.class,
                    () -> choose(strategy, "R5k1/5ppp/8/8/8/8/8/4K3 b - - 0 1"));  // checkmated
            assertThrows(IllegalStateException.class,
                    () -> choose(strategy, "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1"));     // stalemated
        }
    }

    @Test
    void depthMustBeAtLeastOne() {
        IntFunction<SearchStrategy> makeMinimax = SearchStrategyTest::minimax;
        IntFunction<SearchStrategy> makeAlphaBeta = SearchStrategyTest::alphaBeta;
        assertThrows(IllegalArgumentException.class, () -> makeMinimax.apply(0));
        assertThrows(IllegalArgumentException.class, () -> makeAlphaBeta.apply(-1));
    }

    @Test
    void namesSayTheAlgorithmAndDepth() {
        assertEquals("minimax, depth 3", minimax(3).getName());
        assertEquals("alpha-beta, depth 4", alphaBeta(4).getName());
        assertEquals(4, alphaBeta(4).getDepth());
    }

    // ------------------------------------------------------- move ordering

    @Test
    void capturesAreTriedBeforeQuietMoves() {
        Move capture = new Move(Position.of("e4"), Position.of("e5"), new Rook(PieceColor.WHITE),
                new Pawn(PieceColor.BLACK));
        Move quiet = new Move(Position.of("e4"), Position.of("e3"), new Rook(PieceColor.WHITE), null);

        assertEquals(0, AlphaBetaStrategy.priority(quiet));
        assertTrue(AlphaBetaStrategy.priority(capture) > 0);
    }

    @Test
    void biggerVictimComesFirstAndThenTheCheaperAttacker() {
        Position from = Position.of("c4");
        Position to = Position.of("d5");
        int pawnTakesQueen = AlphaBetaStrategy.priority(
                new Move(from, to, new Pawn(PieceColor.WHITE), new Queen(PieceColor.BLACK)));
        int rookTakesQueen = AlphaBetaStrategy.priority(
                new Move(from, to, new Rook(PieceColor.WHITE), new Queen(PieceColor.BLACK)));
        int pawnTakesRook = AlphaBetaStrategy.priority(
                new Move(from, to, new Pawn(PieceColor.WHITE), new Rook(PieceColor.BLACK)));
        int queenTakesPawn = AlphaBetaStrategy.priority(
                new Move(from, to, new Queen(PieceColor.WHITE), new Pawn(PieceColor.BLACK)));

        assertTrue(pawnTakesQueen > rookTakesQueen);
        assertTrue(rookTakesQueen > pawnTakesRook);   // the victim matters more than the attacker
        assertTrue(pawnTakesRook > queenTakesPawn);
        assertTrue(queenTakesPawn > 0);
    }

    @Test
    void promotionsAreTriedEarly() {
        Position from = Position.of("a7");
        Position to = Position.of("a8");
        Pawn pawn = new Pawn(PieceColor.WHITE);
        int toQueen = AlphaBetaStrategy.priority(new Move(from, to, pawn, null, PieceType.QUEEN));
        int toKnight = AlphaBetaStrategy.priority(new Move(from, to, pawn, null, PieceType.KNIGHT));

        assertTrue(toQueen > toKnight);
        assertTrue(toKnight > 0);
    }
}
