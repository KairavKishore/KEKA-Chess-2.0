package chess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Observer hook: a game tells its listeners when it changes. */
class GameListenerTest {

    private final Game game = new Game();

    /** The FEN of the game each time the listener was told. */
    private final List<String> heard = new ArrayList<>();
    private final GameListener recorder = changed -> heard.add(changed.toFen());

    private Move find(String from, String to) {
        for (Move move : game.legalMovesFrom(Position.of(from))) {
            if (move.getTo().equals(Position.of(to))) {
                return move;
            }
        }
        throw new AssertionError("No legal move from " + from + " to " + to);
    }

    @Test
    void listenerIsToldAfterAMoveWithTheGameAlreadyChanged() {
        game.addListener(recorder);

        game.makeMove(find("e2", "e4"));

        assertEquals(List.of("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"), heard);
    }

    @Test
    void listenerIsHandedTheGameItself() {
        List<Game> games = new ArrayList<>();
        game.addListener(games::add);
        game.makeMove(find("e2", "e4"));
        assertSame(game, games.get(0));
    }

    @Test
    void listenerIsToldAboutUndoAndRedo() {
        game.makeMove(find("e2", "e4"));
        String afterMove = game.toFen();
        game.addListener(recorder);

        game.undo();
        game.redo();

        assertEquals(List.of(Fen.STARTING_POSITION, afterMove), heard);
    }

    @Test
    void listenerIsNotToldWhenAMoveIsRejectedOrNothingCanBeUndone() {
        game.addListener(recorder);
        Move illegal = new Move(Position.of("e2"), Position.of("e5"), new Pawn(PieceColor.WHITE), null);

        assertThrows(IllegalArgumentException.class, () -> game.makeMove(illegal));
        assertThrows(IllegalStateException.class, game::undo);
        assertThrows(IllegalStateException.class, game::redo);

        assertEquals(List.of(), heard);
    }

    @Test
    void everyListenerIsToldInTheOrderTheyWereAdded() {
        List<String> order = new ArrayList<>();
        game.addListener(changed -> order.add("first"));
        game.addListener(changed -> order.add("second"));

        game.makeMove(find("e2", "e4"));

        assertEquals(List.of("first", "second"), order);
    }

    @Test
    void removedListenerHearsNothingMore() {
        game.addListener(recorder);
        game.makeMove(find("e2", "e4"));
        game.removeListener(recorder);

        game.makeMove(find("e7", "e5"));

        assertEquals(1, heard.size());
    }

    @Test
    void listenerMayRemoveItselfWhileBeingTold() {
        GameListener once = new GameListener() {
            @Override
            public void gameChanged(Game changed) {
                heard.add("once");
                changed.removeListener(this);
            }
        };
        game.addListener(once);
        game.addListener(recorder);

        game.makeMove(find("e2", "e4"));
        game.makeMove(find("e7", "e5"));

        assertEquals(3, heard.size());  // "once", then the recorder twice
        assertEquals("once", heard.get(0));
    }

    @Test
    void nullListenerIsRejected() {
        assertThrows(NullPointerException.class, () -> game.addListener(null));
    }
}
