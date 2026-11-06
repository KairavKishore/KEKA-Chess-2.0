package chess.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import chess.ai.RandomStrategy;
import chess.engine.Game;
import chess.engine.Move;
import chess.player.AIPlayer;
import chess.player.Player;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class StatusTextTest {

    private static final Player PERSON = new Player() {
        @Override
        public String getName() {
            return "You";
        }

        @Override
        public boolean isHuman() {
            return true;
        }

        @Override
        public void requestMove(Game game, Consumer<Move> reply) {
        }
    };

    private static final Player COMPUTER = new AIPlayer(new RandomStrategy(), Runnable::run, Runnable::run, 0);

    private static String describe(String fen, Player toMove) {
        return StatusText.describe(Game.fromFen(fen), toMove);
    }

    @Test
    void saysWhoseTurnItIs() {
        assertEquals("White to move.", StatusText.describe(new Game(), PERSON));
        assertEquals("Black to move.",
                describe("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", PERSON));
    }

    @Test
    void saysWhenTheComputerIsThinking() {
        assertEquals("White to move. Computer (random) is thinking…",
                StatusText.describe(new Game(), COMPUTER));
    }

    @Test
    void saysWhoIsInCheck() {
        String bishopCheck = "rnbqkbnr/ppp1pppp/8/1B1p4/4P3/8/PPPP1PPP/RNBQK1NR b KQkq - 1 2";
        assertEquals("Black to move. Black is in check.", describe(bishopCheck, PERSON));
        assertEquals("Black to move. Black is in check. Computer (random) is thinking…",
                describe(bishopCheck, COMPUTER));
    }

    @Test
    void namesTheWinnerOfACheckmate() {
        assertEquals("Checkmate. White wins.", describe("R5k1/5ppp/8/8/8/8/8/4K3 b - - 0 1", PERSON));
        assertEquals("Checkmate. Black wins.",
                describe("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3", COMPUTER));
    }

    @Test
    void explainsEachKindOfDraw() {
        assertEquals("Stalemate: Black has no legal move. The game is a draw.",
                describe("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1", PERSON));
        assertEquals("Draw: not enough pieces left to checkmate.",
                describe("8/8/8/4k3/8/8/4K3/8 w - - 0 1", PERSON));
        assertEquals("Draw: fifty moves without a capture or pawn move.",
                describe("8/8/8/4k3/8/8/4K3/R7 b - - 100 80", PERSON));
    }

    @Test
    void explainsADrawByRepetition() {
        Game game = new Game();
        String[][] shuffle = {{"g1", "f3"}, {"g8", "f6"}, {"f3", "g1"}, {"f6", "g8"}};
        for (int round = 0; round < 2; round++) {
            for (String[] squares : shuffle) {
                for (Move move : game.legalMoves()) {
                    if (move.toString().equals(squares[0] + squares[1])) {
                        game.makeMove(move);
                        break;
                    }
                }
            }
        }
        assertEquals("Draw: the same position three times.", StatusText.describe(game, COMPUTER));
    }
}
