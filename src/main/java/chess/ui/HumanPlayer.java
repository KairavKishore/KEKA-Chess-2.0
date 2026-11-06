package chess.ui;

import chess.engine.Game;
import chess.engine.Move;
import chess.player.Player;

import java.util.function.Consumer;

/** A person, who chooses moves by clicking on the board. */
public class HumanPlayer implements Player {

    private final BoardView boardView;

    /**
     * @param boardView the board this person clicks on
     */
    public HumanPlayer(BoardView boardView) {
        this.boardView = boardView;
    }

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
        boardView.requestMove(reply);
    }
}
