package chess.ui;

import javafx.scene.paint.Color;

/** A colour scheme for the board and pieces. */
public enum BoardTheme {
    /** The house colours: a red and yellow board in a black frame. */
    KEKA("Keka red and yellow", "#f6c945", "#b3201a", "#141414", "#ffd60a", "#ffd60a"),
    CLASSIC("Classic wood", "#f0d9b5", "#b58863", "#4a2f1b", "#f0d9b5", "#1c1c1c"),
    MIDNIGHT("Midnight", "#cfcfcf", "#4a4a4a", "#0a0a0a", "#e10600", "#e10600"),
    /** The colours of cheating chess: white and purple squares, trimmed in green. */
    BLACK_MARKET("Black market", "#f4f0fa", "#7b2cbf", "#12081c", "#39ff14", "#39ff14"),
    /** The colours of angry chaotic mode: red and black, and nothing friendly. */
    DANGER("Danger", "#7a7a7a", "#9b0000", "#000000", "#ff1a1a", "#ffffff");

    private final String label;
    private final Color lightSquare;
    private final Color darkSquare;
    private final Color frame;
    private final Color accent;
    private final Color blackPieceOutline;

    BoardTheme(String label, String lightSquare, String darkSquare, String frame, String accent,
            String blackPieceOutline) {
        this.label = label;
        this.lightSquare = Color.web(lightSquare);
        this.darkSquare = Color.web(darkSquare);
        this.frame = Color.web(frame);
        this.accent = Color.web(accent);
        this.blackPieceOutline = Color.web(blackPieceOutline);
    }

    public Color getLightSquare() {
        return lightSquare;
    }

    public Color getDarkSquare() {
        return darkSquare;
    }

    /** The border around the squares. */
    public Color getFrame() {
        return frame;
    }

    /** The trim of the frame and the coordinate labels. */
    public Color getAccent() {
        return accent;
    }

    /** The outline that keeps a black piece visible on a dark square. */
    public Color getBlackPieceOutline() {
        return blackPieceOutline;
    }

    /** The name to show in a list of themes. */
    @Override
    public String toString() {
        return label;
    }
}
