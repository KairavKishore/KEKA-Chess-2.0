package chess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Perft ("performance test") counts every sequence of legal moves of a given
 * length from a position. The counts are published, so matching them is the
 * standard way to prove a move generator correct: one missing or extra move
 * anywhere changes the total.
 *
 * <p>From the starting position the counts are 20, 400, 8,902, 197,281,
 * 4,865,609, ... The first three need only basic piece movement. Depth 4 is
 * the first with checkmates and depth 5 the first with en passant captures.
 *
 * <p>Nobody castles or promotes within five moves of the start, so the other
 * positions here are the standard ones chosen to exercise those rules. They
 * come from the Chess Programming Wiki's "Perft Results" page.
 *
 * <p>The search plays and takes back every move on a single board, so these
 * tests check {@link Board#undoMove} as thoroughly as the move generator: an
 * undo that restored anything wrongly would change a count, or leave the
 * board different from how it started.
 */
class PerftTest {

    private static long perft(Board board, PieceColor sideToMove, int depth) {
        List<Move> moves = board.legalMoves(sideToMove);
        if (depth == 1) {
            return moves.size();  // no need to play the last move just to count it
        }
        long count = 0;
        for (Move move : moves) {
            board.makeMove(move);
            count += perft(board, sideToMove.opposite(), depth - 1);
            board.undoMove(move);
        }
        return count;
    }

    /** The same count, trying each move on a copy of the board instead of undoing it. */
    private static long perftByCopying(Board board, PieceColor sideToMove, int depth) {
        if (depth == 0) {
            return 1;
        }
        long count = 0;
        for (Move move : board.legalMoves(sideToMove)) {
            Board next = new Board(board);
            next.makeMove(move);
            count += perftByCopying(next, sideToMove.opposite(), depth - 1);
        }
        return count;
    }

    /**
     * Checks the counts at depths 1, 2, 3, ... from a position, and that the
     * search leaves the position exactly as it found it.
     */
    private static void assertPerft(String fen, long... expectedCounts) {
        Game game = Game.fromFen(fen);
        for (int depth = 1; depth <= expectedCounts.length; depth++) {
            assertEquals(expectedCounts[depth - 1], perft(game.getBoard(), game.getTurn(), depth),
                    "depth " + depth);
            assertEquals(fen, game.toFen(), "position after searching to depth " + depth);
        }
    }

    @Test
    void startingPosition() {
        assertPerft(Fen.STARTING_POSITION, 20, 400, 8902, 197281, 4865609);
    }

    /** "Kiwipete": both sides can castle either way, with pins, checks and en passant. */
    @Test
    void castlingOnBothSides() {
        assertPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
                48, 2039, 97862, 4085603);
    }

    /** An endgame where en passant captures would expose the capturing side's own king. */
    @Test
    void enPassantAndPinsInAnEndgame() {
        assertPerft("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
                14, 191, 2812, 43238, 674624);
    }

    /** White starts in check, and pawns on both sides are about to promote. */
    @Test
    void promotionsWhileInCheck() {
        assertPerft("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
                6, 264, 9467, 422333);
    }

    /** The previous position with the colours swapped, so the counts are the same with Black to move. */
    @Test
    void promotionsWhileInCheckWithBlackToMove() {
        assertPerft("r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1",
                6, 264, 9467, 422333);
    }

    /** A promotion by capture is available on the first move, with castling rights for White only. */
    @Test
    void promotionByCapture() {
        assertPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
                44, 1486, 62379, 2103487);
    }

    /** A crowded middlegame with no special moves nearby: many pieces, pins and captures. */
    @Test
    void crowdedMiddlegame() {
        assertPerft("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
                46, 2079, 89890, 3894594);
    }

    @Test
    void undoingAndCopyingGiveTheSameCounts() {
        Game game = Game.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        assertEquals(97862L, perftByCopying(game.getBoard(), game.getTurn(), 3));
        assertEquals(97862L, perft(game.getBoard(), game.getTurn(), 3));
    }
}
