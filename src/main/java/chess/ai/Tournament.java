package chess.ai;

import chess.engine.Board;
import chess.engine.Game;
import chess.engine.GameStatus;
import chess.engine.Move;
import chess.engine.PieceColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Plays strategies against each other many times, to measure which is
 * stronger and how much work each does. Run it from the project folder with
 *
 * <pre>
 * mvn -q compile
 * java -cp target/classes chess.ai.Tournament
 * </pre>
 *
 * <p>The searching strategies have no randomness, so two of them would play
 * the same game every time. To get different games, each one starts with a
 * few random moves. Each random opening is played twice, with the strategies
 * swapping colours, so that neither benefits from a lucky opening.
 */
public final class Tournament {

    /** Random moves played before the strategies take over. */
    private static final int OPENING_PLIES = 4;

    /** A game still going after this many half-moves is called a draw. */
    private static final int MAX_PLIES = 400;

    private Tournament() {
    }

    /** The outcome of a match between two strategies. */
    public static final class Result {
        private final MeasuredStrategy first;
        private final MeasuredStrategy second;
        private int firstWins;
        private int secondWins;
        private int draws;

        Result(MeasuredStrategy first, MeasuredStrategy second) {
            this.first = first;
            this.second = second;
        }

        /** The first strategy, with the figures for all the moves it chose. */
        public MeasuredStrategy getFirst() {
            return first;
        }

        /** The second strategy, with the figures for all the moves it chose. */
        public MeasuredStrategy getSecond() {
            return second;
        }

        public int getFirstWins() {
            return firstWins;
        }

        public int getSecondWins() {
            return secondWins;
        }

        public int getDraws() {
            return draws;
        }

        public int getGames() {
            return firstWins + secondWins + draws;
        }

        /**
         * The first strategy's score as a percentage, counting a win as one
         * point and a draw as half, the way chess tournaments do.
         */
        public double getFirstScorePercent() {
            return getGames() == 0 ? 0 : 100.0 * (firstWins + 0.5 * draws) / getGames();
        }
    }

    /**
     * Plays a match of {@code 2 * openings} games.
     *
     * @param openings how many random openings to use; each is played twice
     * @param seed     decides the openings, so a match can be repeated exactly
     */
    public static Result play(MoveStrategy first, MoveStrategy second, int openings, long seed) {
        Result result = new Result(new MeasuredStrategy(first), new MeasuredStrategy(second));
        Random random = new Random(seed);
        for (int i = 0; i < openings; i++) {
            List<Move> opening = randomOpening(random);
            record(result, playGame(result.first, result.second, opening), PieceColor.WHITE);
            record(result, playGame(result.second, result.first, opening), PieceColor.BLACK);
        }
        return result;
    }

    /**
     * @param winner      the colour that won, or null for a draw
     * @param firstColour the colour the first strategy had in that game
     */
    private static void record(Result result, PieceColor winner, PieceColor firstColour) {
        if (winner == null) {
            result.draws++;
        } else if (winner == firstColour) {
            result.firstWins++;
        } else {
            result.secondWins++;
        }
    }

    /** A few random legal moves from the starting position, after which the game is still going. */
    private static List<Move> randomOpening(Random random) {
        MoveStrategy mover = new RandomStrategy(random);
        while (true) {
            Game game = new Game();
            for (int ply = 0; ply < OPENING_PLIES && !game.getStatus().isGameOver(); ply++) {
                game.makeMove(mover.chooseMove(new Board(game.getBoard()), game.getTurn()));
            }
            if (!game.getStatus().isGameOver()) {
                return game.getMoveHistory();
            }
        }
    }

    /**
     * Plays one game from the position after the opening moves.
     *
     * @return the colour that won, or null for a draw
     */
    static PieceColor playGame(MoveStrategy white, MoveStrategy black, List<Move> opening) {
        Game game = new Game();
        for (Move move : opening) {
            game.makeMove(move);
        }
        while (!game.getStatus().isGameOver() && game.getMoveHistory().size() < MAX_PLIES) {
            MoveStrategy toMove = game.getTurn() == PieceColor.WHITE ? white : black;
            // Each strategy gets its own copy of the board, as it does in the application.
            game.makeMove(toMove.chooseMove(new Board(game.getBoard()), game.getTurn()));
        }
        if (game.getStatus() == GameStatus.CHECKMATE) {
            return game.getTurn().opposite();  // the side to move has been mated
        }
        return null;
    }

    /** The same strategy under another name, to tell apart two that differ only in their evaluator. */
    private static MoveStrategy renamed(String name, MoveStrategy strategy) {
        return new MoveStrategy() {
            @Override
            public Move chooseMove(Board board, PieceColor sideToMove) {
                return strategy.chooseMove(board, sideToMove);
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public long getNodesSearched() {
                return strategy.getNodesSearched();
            }
        };
    }

    /** One line of the results table. */
    static String describe(Result result) {
        return String.format(Locale.ROOT, "%-22s %-22s %3d %3d %3d %5.0f%%  %9.1f %10.0f  %9.1f %10.0f",
                result.first.getName(), result.second.getName(),
                result.firstWins, result.draws, result.secondWins, result.getFirstScorePercent(),
                result.first.getAverageMillis(), result.first.getAverageNodes(),
                result.second.getAverageMillis(), result.second.getAverageNodes());
    }

    static String header() {
        return String.format(Locale.ROOT, "%-22s %-22s %3s %3s %3s %6s  %9s %10s  %9s %10s",
                "First", "Second", "W", "D", "L", "Score", "ms/move", "nodes/move", "ms/move", "nodes/move");
    }

    /**
     * Runs a set of matches and prints a table. The optional argument is the
     * number of random openings per match (the default is 10, giving 20 games).
     */
    public static void main(String[] args) {
        int openings = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Evaluator positional = new PieceSquareEvaluator();

        List<MoveStrategy[]> pairings = new ArrayList<>();
        // Does searching beat not searching, and does searching deeper beat searching less?
        pairings.add(new MoveStrategy[] {new AlphaBetaStrategy(2, positional), new RandomStrategy(new Random(1))});
        pairings.add(new MoveStrategy[] {new AlphaBetaStrategy(4, positional), new AlphaBetaStrategy(2, positional)});
        pairings.add(new MoveStrategy[] {new AlphaBetaStrategy(5, positional), new AlphaBetaStrategy(4, positional)});
        // Do the piece-square tables help, at the same depth?
        pairings.add(new MoveStrategy[] {
            renamed("depth 4, with tables", new AlphaBetaStrategy(4, positional)),
            renamed("depth 4, material only", new AlphaBetaStrategy(4, new MaterialEvaluator()))});
        // Minimax and alpha-beta find the same scores, so they should be level, at very different cost.
        pairings.add(new MoveStrategy[] {new AlphaBetaStrategy(3, positional), new MinimaxStrategy(3, positional)});

        System.out.println(openings * 2 + " games per match. W, D and L are the first strategy's results.");
        System.out.println(header());
        for (MoveStrategy[] pairing : pairings) {
            System.out.println(describe(play(pairing[0], pairing[1], openings, 2024)));
        }
    }
}
