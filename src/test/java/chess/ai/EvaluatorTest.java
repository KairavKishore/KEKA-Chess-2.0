package chess.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.engine.Fen;
import chess.engine.Game;
import chess.engine.PieceType;
import org.junit.jupiter.api.Test;

class EvaluatorTest {

    private final Evaluator material = new MaterialEvaluator();
    private final Evaluator positional = new PieceSquareEvaluator();

    private static int score(Evaluator evaluator, String fen) {
        return evaluator.evaluate(Game.fromFen(fen).getBoard());
    }

    /**
     * The same position with the colours swapped: the board is turned upside
     * down and every piece changes sides.
     */
    static String swapColours(String fen) {
        String[] fields = fen.split(" ");
        String[] ranks = fields[0].split("/");
        StringBuilder placement = new StringBuilder();
        for (int i = ranks.length - 1; i >= 0; i--) {
            for (char letter : ranks[i].toCharArray()) {
                placement.append(Character.isUpperCase(letter)
                        ? Character.toLowerCase(letter) : Character.toUpperCase(letter));
            }
            if (i > 0) {
                placement.append('/');
            }
        }
        String turn = fields[1].equals("w") ? "b" : "w";
        return placement + " " + turn + " - - 0 1";
    }

    private static final String[] POSITIONS = {
        Fen.STARTING_POSITION,
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
        "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
        "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
        "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
    };

    // ------------------------------------------------------------ material

    @Test
    void pieceValuesAreTheStandardOnes() {
        assertEquals(100, MaterialEvaluator.valueOf(PieceType.PAWN));
        assertEquals(320, MaterialEvaluator.valueOf(PieceType.KNIGHT));
        assertEquals(330, MaterialEvaluator.valueOf(PieceType.BISHOP));
        assertEquals(500, MaterialEvaluator.valueOf(PieceType.ROOK));
        assertEquals(900, MaterialEvaluator.valueOf(PieceType.QUEEN));
        assertEquals(0, MaterialEvaluator.valueOf(PieceType.KING));
    }

    @Test
    void startingPositionIsLevel() {
        assertEquals(0, score(material, Fen.STARTING_POSITION));
        assertEquals(0, score(positional, Fen.STARTING_POSITION));
    }

    @Test
    void materialIsWhitesPiecesMinusBlacks() {
        assertEquals(900, score(material, "4k3/8/8/8/8/8/8/3QK3 w - - 0 1"));
        assertEquals(-500, score(material, "r3k3/8/8/8/8/8/8/4K3 w - - 0 1"));
        assertEquals(330 - 320, score(material, "4k1n1/8/8/8/8/8/8/2B1K3 w - - 0 1"));
        assertEquals(100, score(material, "4k3/pppp4/8/8/8/8/PPPPP3/4K3 w - - 0 1"));
    }

    @Test
    void materialDoesNotCareWhoseTurnItIs() {
        assertEquals(score(material, "4k3/8/8/8/8/8/8/3QK3 w - - 0 1"),
                score(material, "4k3/8/8/8/8/8/8/3QK3 b - - 0 1"));
    }

    // -------------------------------------------------- piece-square tables

    @Test
    void knightIsWorthMoreInTheCentreThanInTheCorner() {
        int centre = score(positional, "4k3/8/8/8/3N4/8/8/4K3 w - - 0 1");
        int corner = score(positional, "4k3/8/8/8/8/8/8/N3K3 w - - 0 1");
        assertEquals(20 - (-50), centre - corner);
    }

    @Test
    void pawnGainsValueAsItAdvances() {
        int onSecond = score(positional, "4k3/8/8/8/8/8/P7/4K3 w - - 0 1");
        int onSeventh = score(positional, "4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        assertTrue(onSeventh > onSecond);
        assertEquals(50 - 5, onSeventh - onSecond);
    }

    @Test
    void blackReadsTheTablesFromItsOwnSide() {
        // A black pawn one step from promoting, on its seventh rank (rank 2).
        int advanced = score(positional, "4k3/8/8/8/8/8/p7/4K3 w - - 0 1");
        int unmoved = score(positional, "4k3/p7/8/8/8/8/8/4K3 w - - 0 1");
        assertEquals(-(50 - 5), advanced - unmoved);
    }

    @Test
    void castledKingScoresBetterThanAKingInTheCentre() {
        int castled = score(positional, "4k3/8/8/8/8/8/8/6K1 w - - 0 1");
        int central = score(positional, "4k3/8/8/8/3K4/8/8/8 w - - 0 1");
        assertTrue(castled > central);
    }

    @Test
    void developingMovesImproveTheScore() {
        int start = score(positional, Fen.STARTING_POSITION);
        int afterE4 = score(positional, "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        int afterNf3 = score(positional, "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1");
        int afterNa3 = score(positional, "rnbqkbnr/pppppppp/8/8/8/N7/PPPPPPPP/R1BQKBNR b KQkq - 1 1");
        assertTrue(afterE4 > start);
        assertTrue(afterNf3 > start);
        assertTrue(afterNf3 > afterNa3);  // a knight on the rim is dim
    }

    @Test
    void swappingTheColoursNegatesTheScore() {
        for (String fen : POSITIONS) {
            String swapped = swapColours(fen);
            assertEquals(-score(material, fen), score(material, swapped), fen);
            assertEquals(-score(positional, fen), score(positional, swapped), fen);
        }
    }

    @Test
    void positionalScoreStaysCloseToTheMaterialCount() {
        // The tables only nudge: they must never outweigh winning a piece.
        for (String fen : POSITIONS) {
            int difference = Math.abs(score(positional, fen) - score(material, fen));
            assertTrue(difference < 300, fen + " differs by " + difference);
        }
    }
}
