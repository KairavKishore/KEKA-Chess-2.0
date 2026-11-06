package chess.ui;

import chess.engine.Board;
import chess.engine.Game;
import chess.engine.Move;
import chess.cheat.CheatGame.SquareState;
import chess.engine.Piece;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import chess.engine.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * The chess board on screen, drawn from a {@link BoardSource}. It can be
 * shown flat or in pseudo-3D, from either player's side, in several colour
 * themes.
 *
 * <p>The view holds no chess rules of its own, and it never changes the
 * game. It draws what its source tells it, and must be told to
 * {@link #refresh()} when the game changes. When asked for a move with
 * {@link #requestMove}, it lets the player click a piece and then one of the
 * squares that piece can go to, and passes the chosen move on.
 *
 * <p>Every square is a four-sided shape whose corners come from a
 * {@link Projection}. In the flat view they are plain squares; in the 3D
 * view they are the same squares seen in perspective. The pieces are drawn
 * on top, standing upright, from the far side of the board to the near side
 * so that nearer pieces overlap the ones behind them.
 */
public class BoardView extends StackPane implements BoardInput {

    /** The width and height of the board area, in pixels. */
    public static final double SIZE = 560;

    /** The width of the frame around the squares, in squares. */
    private static final double FRAME = 0.36;

    private static final Color SELECTED_BLEND = Color.WHITE;
    private static final Color LAST_MOVE_BLEND = Color.web("#ff7a00");
    private static final Color CHECK_COLOR = Color.web("#ff2d2d");
    private static final Color MARK = Color.rgb(0, 0, 0, 0.45);
    private static final Color HONEST_MARK = Color.web("#00c853", 0.9);
    private static final Color CHEAT_MARK = Color.web("#ff1744", 0.9);
    private static final Color CRATER_COLOR = Color.web("#0b0806");
    private static final Color SAFE_COLOR = Color.web("#00e5ff");
    private static final LinearGradient LAVA_FILL = new LinearGradient(0, 0, 1, 1, true, CycleMethod.REFLECT,
            new Stop(0, Color.web("#ffb300")), new Stop(0.5, Color.web("#ff3d00")), new Stop(1, Color.web("#b71c1c")));

    private final BoardSource source;
    private final Selection selection;

    private final Pane canvas = new Pane();
    private final Label banner = new Label();
    private final Group pieceLayer = new Group();
    private final Group markLayer = new Group();

    /** Rain, and the blackout of half the board: things drawn over everything else. */
    private final Group weatherLayer = new Group();

    /** Shown in place of the source's banner when not null: for endings the game does not know about. */
    private String bannerOverride;

    /** How often the pieces jump about while the board is scrambled. */
    private static final double SCRAMBLE_MILLIS = 450;

    /**
     * While the board is scrambled, the square whose piece is drawn on each
     * square: square number i shows what really stands on square number
     * scramble.get(i). Null when the board is drawn truthfully.
     */
    private List<Integer> scramble;
    private final Timeline scrambler = new Timeline(new KeyFrame(Duration.millis(SCRAMBLE_MILLIS), event -> {
        Collections.shuffle(scramble);
        redraw();
    }));

    /** Indexed as squares[rank][file], like the engine's board. */
    private final Polygon[][] squares = new Polygon[Board.SIZE][Board.SIZE];

    private Projection projection;
    private BoardTheme theme = BoardTheme.KEKA;
    private boolean showHints = true;

    /** Where to send the move the player chooses, or null while no move is wanted. */
    private Consumer<Move> pendingReply;

    /** Where to send clicked squares instead of treating them as moves, or null. */
    private Consumer<Position> picker;

    /** A square marked as picked while squares are being pointed at, or null. */
    private Position picked;

    /** Picks one of the four moves of a promoting pawn; returns null if the player backs out. */
    private Function<List<Move>, Move> promotionChooser =
            candidates -> PromotionDialog.choose(getScene().getWindow(), candidates, theme);

    /** A board for an ordinary game of chess, which redraws itself whenever the game changes. */
    public BoardView(Game game) {
        this(new GameBoardSource(game));
        game.addListener(changed -> refresh());
    }

    public BoardView(BoardSource source) {
        this.source = source;
        this.selection = new Selection(source);
        this.projection = new Projection(SIZE, SIZE, true, false);

        canvas.setPrefSize(SIZE, SIZE);
        canvas.setMinSize(SIZE, SIZE);
        canvas.setMaxSize(SIZE, SIZE);
        // The pieces and marks are only pictures: clicks pass through them to the square underneath.
        pieceLayer.setMouseTransparent(true);
        markLayer.setMouseTransparent(true);
        weatherLayer.setMouseTransparent(true);
        banner.setMouseTransparent(true);
        banner.getStyleClass().add("banner");
        // At the top, where it does not hide the position that ended the game.
        StackPane.setAlignment(banner, Pos.TOP_CENTER);
        StackPane.setMargin(banner, new Insets(10, 0, 0, 0));
        getChildren().addAll(canvas, banner);

        rebuild();
    }

    // ------------------------------------------------------------- options

    /** Switches between the pseudo-3D view and the flat view from above. */
    public void setPerspective(boolean perspective) {
        projection = new Projection(SIZE, SIZE, perspective, projection.isFlipped());
        rebuild();
    }

    /** Shows the board from Black's side (true) or White's (false). */
    public void setFlipped(boolean flipped) {
        projection = new Projection(SIZE, SIZE, projection.isPerspective(), flipped);
        rebuild();
    }

    public void setTheme(BoardTheme theme) {
        this.theme = theme;
        rebuild();
    }

    /**
     * Scrambles the board (true) or puts it right (false). While it is
     * scrambled every piece is drawn on a random square, changing a couple
     * of times a second, and clicks do nothing. Only the picture is wrong;
     * the game underneath is untouched.
     */
    public void setScrambled(boolean scrambled) {
        scrambler.stop();
        scramble = null;
        if (scrambled) {
            scramble = new ArrayList<>();
            for (int square = 0; square < Board.SIZE * Board.SIZE; square++) {
                scramble.add(square);
            }
            Collections.shuffle(scramble);
            scrambler.setCycleCount(Timeline.INDEFINITE);
            scrambler.play();
            selection.clear();
        }
        redraw();
    }

    /** Whether to mark the squares the selected piece can move to. */
    public void setShowHints(boolean showHints) {
        this.showHints = showHints;
        redraw();
    }

    // -------------------------------------------------------------- input

    /**
     * Lets the player choose a move for the side whose turn it is by
     * clicking, and sends it to {@code reply}. Until this is called, and
     * again once a move has been chosen, clicks on the board do nothing.
     */
    @Override
    public void requestMove(Consumer<Move> reply) {
        pendingReply = reply;
    }

    @Override
    public void pickSquares(Consumer<Position> picker) {
        this.picker = picker;
        selection.clear();
        picked = null;
        redraw();
    }

    @Override
    public void highlight(Position position) {
        picked = position;
        redraw();
    }

    /** Replaces the promotion dialog, so that tests can choose without a window. */
    void setPromotionChooser(Function<List<Move>, Move> promotionChooser) {
        this.promotionChooser = promotionChooser;
    }

    /** A click on a square, from the mouse. Package-private so it can be driven without one. */
    void handleClick(Position clicked) {
        if (scramble != null) {
            return;  // hacked: nothing on the board can be trusted, or touched
        }
        // Under the "backwards" debuff a click lands on the opposite square of the board.
        Position position = source.isBackwards()
                ? new Position(Board.SIZE - 1 - clicked.getFile(), Board.SIZE - 1 - clicked.getRank())
                : clicked;
        if (picker != null) {
            picker.accept(position);
            return;
        }
        if (pendingReply == null) {
            return;  // not a human's turn, or the game is over
        }
        List<Move> candidates = selection.click(position);
        redraw();
        if (candidates.isEmpty()) {
            return;
        }
        Move move = candidates.size() == 1 ? candidates.get(0) : promotionChooser.apply(candidates);
        if (move == null) {
            return;  // the promotion dialog was closed without choosing
        }
        Consumer<Move> reply = pendingReply;
        pendingReply = null;
        reply.accept(move);
    }

    /** The game has changed: forget any half-made choice and show the new position. */
    @Override
    public void refresh() {
        selection.clear();
        redraw();
    }

    // ------------------------------------------------------------ drawing

    /** Lays the board out again from scratch. Needed when the view or the theme changes. */
    private void rebuild() {
        Group squareLayer = new Group();
        for (int rank = 0; rank < Board.SIZE; rank++) {
            for (int file = 0; file < Board.SIZE; file++) {
                Position position = new Position(file, rank);
                int column = projection.column(position);
                int row = projection.row(position);
                Polygon square = new Polygon(projection.outline(column, row, column + 1, row + 1));
                square.setOnMouseClicked(event -> handleClick(position));
                square.setOnMouseEntered(event -> square.setOpacity(0.82));
                square.setOnMouseExited(event -> square.setOpacity(1));
                squares[rank][file] = square;
                squareLayer.getChildren().add(square);
            }
        }
        canvas.getChildren().setAll(buildFrame(), squareLayer, markLayer, pieceLayer, weatherLayer);
        redraw();
    }

    /** The border around the squares with its coordinate labels and, in 3D, the front edge of the slab. */
    private Group buildFrame() {
        Group frame = new Group();
        double far = Board.SIZE + FRAME;

        if (projection.isPerspective()) {
            // The thickness of the board: the frame's near edge, dropped straight down.
            double thickness = projection.getUnit() * 0.3;
            double left = projection.screenX(-FRAME, -FRAME);
            double right = projection.screenX(far, -FRAME);
            double top = projection.screenY(-FRAME);
            Polygon edge = new Polygon(left, top, right, top, right, top + thickness, left, top + thickness);
            edge.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, theme.getFrame().interpolate(Color.WHITE, 0.18)),
                    new Stop(1, theme.getFrame().interpolate(Color.BLACK, 0.6))));
            edge.setStroke(theme.getAccent());
            edge.setEffect(new DropShadow(26, 0, 14, Color.rgb(0, 0, 0, 0.7)));
            frame.getChildren().add(edge);
        }

        Polygon border = new Polygon(projection.outline(-FRAME, -FRAME, far, far));
        border.setFill(theme.getFrame());
        border.setStroke(theme.getAccent());
        border.setStrokeWidth(2);
        if (!projection.isPerspective()) {
            border.setEffect(new DropShadow(22, 0, 8, Color.rgb(0, 0, 0, 0.7)));
        }
        frame.getChildren().add(border);

        for (int i = 0; i < Board.SIZE; i++) {
            // The square in column i of the near row gives the file letter;
            // the square in row i of the left column gives the rank number.
            String file = projection.positionAt(i, 0).toString().substring(0, 1);
            String rank = projection.positionAt(0, i).toString().substring(1);
            frame.getChildren().add(label(file, i + 0.5, -FRAME / 2));
            frame.getChildren().add(label(rank, -FRAME / 2, i + 0.5));
        }
        return frame;
    }

    /** A coordinate label centred on a point of the board. */
    private Text label(String text, double x, double depth) {
        Text label = new Text(text);
        label.setFont(Font.font("DejaVu Sans", FontWeight.BOLD,
                projection.getUnit() * 0.2 * projection.scale(depth)));
        label.setFill(theme.getAccent());
        label.setTextOrigin(VPos.CENTER);
        label.setX(projection.screenX(x, depth) - label.getLayoutBounds().getWidth() / 2);
        label.setY(projection.screenY(depth));
        return label;
    }

    /** Updates the squares, marks, pieces and banner to match the game. */
    private void redraw() {
        Move lastMove = source.getLastMove();
        Position checkedKing = source.getAlarmSquare();
        markLayer.getChildren().clear();
        pieceLayer.getChildren().clear();

        // From the far row to the near one, so that nearer pieces are drawn over those behind.
        for (int row = Board.SIZE - 1; row >= 0; row--) {
            for (int column = 0; column < Board.SIZE; column++) {
                Position position = projection.positionAt(column, row);
                // When scrambled, this square shows what really stands on some other square.
                Position shown = scramble == null ? position : unscrambled(position);
                Piece piece = source.pieceAt(shown);

                paintSquare(position, lastMove, checkedKing);
                List<Move> movesHere = selection.movesTo(position);
                if (showHints && !movesHere.isEmpty()) {
                    // A ring leaves the piece that would be captured visible. An
                    // en passant capture lands on an empty square, so it gets a dot.
                    Color colour = markColour(movesHere.get(0));
                    markLayer.getChildren().add(piece == null ? dot(column, row, colour) : ring(column, row, colour));
                }
                if (piece != null) {
                    addPiece(piece, column, row, source.secretAt(shown));
                }
            }
        }
        drawWeather();
        showBanner();
    }

    /** The square whose piece is being drawn on the given square while the board is scrambled. */
    private Position unscrambled(Position position) {
        int from = scramble.get(position.getRank() * Board.SIZE + position.getFile());
        return new Position(from % Board.SIZE, from / Board.SIZE);
    }

    /** Sets an announcement to show over the board whatever the game says, or null for none. */
    public void setBannerOverride(String text) {
        bannerOverride = text;
        showBanner();
    }

    /** Rain over the whole board in a storm, heavier the worse it is, and the blackout debuff. */
    private void drawWeather() {
        weatherLayer.getChildren().clear();
        int severity = source.getStormSeverity();
        if (severity > 0) {
            Rectangle gloom = new Rectangle(SIZE, SIZE, Color.rgb(20, 40, 90, 0.10 + 0.05 * severity));
            weatherLayer.getChildren().add(gloom);
            // The same seed every time, so the rain does not jump about each time the board is redrawn.
            Random rain = new Random(severity);
            for (int drop = 0; drop < 30 * severity; drop++) {
                double x = rain.nextDouble() * SIZE;
                double y = rain.nextDouble() * SIZE;
                Line streak = new Line(x, y, x - 5, y + 16);
                streak.setStroke(Color.rgb(170, 200, 255, 0.55));
                weatherLayer.getChildren().add(streak);
            }
        }
        if (source.isFoggy()) {
            // A bank of fog over the four middle rows. It is drawn a little
            // beyond them, to cover the pieces standing on the furthest one.
            Polygon fog = new Polygon(projection.outline(-0.2, 2, Board.SIZE + 0.2, 6.6));
            fog.setFill(Color.gray(0.82, 0.96));
            weatherLayer.getChildren().add(fog);
        }
        if (source.isHalfDark()) {
            weatherLayer.getChildren().add(new Rectangle(SIZE / 2, SIZE, Color.BLACK));
        }
    }

    private void paintSquare(Position position, Move lastMove, Position checkedKing) {
        Polygon square = squares[position.getRank()][position.getFile()];
        boolean dark = (position.getFile() + position.getRank()) % 2 == 0;  // a1 is dark
        Color base = dark ? theme.getDarkSquare() : theme.getLightSquare();

        square.setStroke(Color.rgb(0, 0, 0, 0.25));
        square.setStrokeWidth(0.5);
        SquareState state = source.stateAt(position);
        if (state == SquareState.CRATER) {
            square.setFill(CRATER_COLOR);
            square.setStroke(Color.web("#5a4a3a"));
            square.setStrokeWidth(2);
            return;
        }
        if (state == SquareState.LAVA) {
            square.setFill(LAVA_FILL);
            square.setStroke(Color.web("#ffe066"));
            square.setStrokeWidth(2);
            square.toFront();
            return;
        }
        if (position.equals(selection.getSelected()) || position.equals(picked)) {
            square.setFill(base.interpolate(SELECTED_BLEND, 0.6));
            square.setStroke(Color.BLACK);
            square.setStrokeWidth(3);
            square.toFront();  // so its thick outline is not covered by its neighbours
        } else if (position.equals(checkedKing)) {
            square.setFill(CHECK_COLOR);
            square.setStroke(theme.getAccent());
            square.setStrokeWidth(3);
            square.toFront();
        } else if (lastMove != null
                && (position.equals(lastMove.getFrom()) || position.equals(lastMove.getTo()))) {
            square.setFill(base.interpolate(LAST_MOVE_BLEND, 0.55));
        } else {
            square.setFill(base);
        }
        if (state == SquareState.SAFE) {
            square.setStroke(SAFE_COLOR);
            square.setStrokeWidth(4);
            square.toFront();
        }
    }

    /** A circle lying on the board in the middle of a square, with the given radius in squares. */
    private Ellipse discOn(int column, int row, double depthInSquare, double radius) {
        double depth = row + depthInSquare;
        double radiusX = projection.getUnit() * radius * projection.scale(depth);
        return new Ellipse(projection.screenX(column + 0.5, depth), projection.screenY(depth),
                radiusX, radiusX * projection.flatness());
    }

    /**
     * The colour to mark a move in. In cheating chess, green is a move the
     * piece could make as what it looks like, and red is one that only its
     * secret identity allows, which would give it away.
     */
    private Color markColour(Move move) {
        if (!source.marksCheatingMoves()) {
            return MARK;
        }
        return source.isCheatingMove(move) ? CHEAT_MARK : HONEST_MARK;
    }

    private Ellipse dot(int column, int row, Color colour) {
        Ellipse dot = discOn(column, row, 0.5, 0.16);
        dot.setFill(colour);
        dot.setStroke(colour == MARK ? null : Color.rgb(0, 0, 0, 0.7));
        return dot;
    }

    private Ellipse ring(int column, int row, Color colour) {
        Ellipse ring = discOn(column, row, 0.5, 0.42);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(colour);
        ring.setStrokeWidth(projection.getUnit() * 0.08 * projection.scale(row + 0.5));
        return ring;
    }

    /**
     * @param secret what the piece really is, or null if it is what it looks like
     */
    private void addPiece(Piece shown, int column, int row, PieceType secret) {
        // Under the "blob" debuff every piece is drawn white, so the sides cannot be told apart.
        Piece piece = source.isBlob() ? Piece.create(shown.getType(), PieceColor.WHITE) : shown;
        Text text = new Text();
        double size;
        if (projection.isPerspective()) {
            // The piece stands on a point towards the front of its square
            // and rises from there, drawn smaller the further away it is.
            double depth = row + 0.3;
            size = projection.getUnit() * 0.84 * projection.scale(depth);
            PieceArt.paint(text, piece.getType(), piece.getColor(), size, theme);
            text.setTextOrigin(VPos.BASELINE);
            text.setX(projection.screenX(column + 0.5, depth) - text.getLayoutBounds().getWidth() / 2);
            text.setY(projection.screenY(depth));

            Ellipse shadow = discOn(column, row, 0.34, 0.3);
            shadow.setRadiusY(shadow.getRadiusX() * 0.4);
            shadow.setFill(Color.rgb(0, 0, 0, 0.35));
            pieceLayer.getChildren().add(shadow);
        } else {
            size = projection.getUnit() * 0.7;
            PieceArt.paint(text, piece.getType(), piece.getColor(), size, theme);
            text.setTextOrigin(VPos.CENTER);
            text.setX(projection.screenX(column + 0.5, row + 0.5) - text.getLayoutBounds().getWidth() / 2);
            text.setY(projection.screenY(row + 0.5));
        }
        pieceLayer.getChildren().add(text);
        if (secret != null) {
            addSecretBadge(text, secret, size);
        }
    }

    /**
     * A small disc at the piece's shoulder showing what it really is, so
     * the player can tell at a glance which of their pieces are transformed
     * and how each one moves.
     */
    private void addSecretBadge(Text pieceText, PieceType secret, double size) {
        double centreX = pieceText.getBoundsInParent().getMaxX() - size * 0.08;
        double centreY = pieceText.getBoundsInParent().getMinY() + size * 0.26;
        Ellipse disc = new Ellipse(centreX, centreY, size * 0.21, size * 0.21);
        disc.setFill(theme.getAccent());
        disc.setStroke(Color.BLACK);

        Text look = new Text(PieceGlyphs.of(secret));
        look.setFont(Font.font("DejaVu Sans", size * 0.32));
        look.setFill(Color.BLACK);
        look.setTextOrigin(VPos.CENTER);
        look.setX(centreX - look.getLayoutBounds().getWidth() / 2);
        look.setY(centreY);
        pieceLayer.getChildren().addAll(disc, look);
    }

    /** Announces the end of the game above the board. */
    private void showBanner() {
        String text = bannerOverride != null ? bannerOverride : source.getBanner();
        banner.setVisible(text != null);
        banner.setText(text);
    }
}
