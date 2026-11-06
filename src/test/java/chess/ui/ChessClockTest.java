package chess.ui;

import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChessClockTest {

    private final ChessClock clock = new ChessClock(60_000, 2_000);

    @Test
    void bothSidesStartWithTheSameTime() {
        assertEquals(60_000, clock.getRemaining(WHITE));
        assertEquals(60_000, clock.getRemaining(BLACK));
        assertFalse(clock.isOutOfTime(WHITE));
    }

    @Test
    void onlyTheRunningSidesTimeGoesDown() {
        clock.advance(WHITE, 1_500);
        assertEquals(58_500, clock.getRemaining(WHITE));
        assertEquals(60_000, clock.getRemaining(BLACK));
    }

    @Test
    void makingAMoveEarnsTheIncrement() {
        clock.advance(WHITE, 5_000);
        clock.moveMade(WHITE);
        assertEquals(57_000, clock.getRemaining(WHITE));
        assertEquals(60_000, clock.getRemaining(BLACK));
    }

    @Test
    void timeStopsAtZeroAndThatSideIsOut() {
        clock.advance(BLACK, 59_000);
        assertFalse(clock.isOutOfTime(BLACK));
        clock.advance(BLACK, 5_000);
        assertEquals(0, clock.getRemaining(BLACK));
        assertTrue(clock.isOutOfTime(BLACK));
        assertFalse(clock.isOutOfTime(WHITE));
    }

    @Test
    void noIncrementOnceTimeHasRunOut() {
        clock.advance(WHITE, 60_000);
        clock.moveMade(WHITE);
        assertTrue(clock.isOutOfTime(WHITE));
    }

    @Test
    void timeCanBeAddedOrTakenAway() {
        clock.add(WHITE, 20_000);          // a fiddled clock
        assertEquals(80_000, clock.getRemaining(WHITE));
        clock.add(WHITE, -40_000);         // and the penalty for being caught
        assertEquals(40_000, clock.getRemaining(WHITE));
        clock.add(WHITE, -90_000);
        assertEquals(0, clock.getRemaining(WHITE));
    }

    @Test
    void showsMinutesAndSecondsAndTenthsAtTheEnd() {
        assertEquals("5:00", ChessClock.format(300_000));
        assertEquals("4:07", ChessClock.format(247_900));
        assertEquals("0:10", ChessClock.format(10_000));
        assertEquals("0:09.9", ChessClock.format(9_999));
        assertEquals("0:07.3", ChessClock.format(7_340));
        assertEquals("0:00.0", ChessClock.format(0));
        assertEquals("12:30", ChessClock.format(750_000));
    }

    @Test
    void timeControlsBuildTheirClocks() {
        assertNull(TimeControl.NONE.createClock());
        ChessClock rapid = TimeControl.RAPID.createClock();
        assertNotNull(rapid);
        assertEquals(300_000, rapid.getRemaining(WHITE));
        rapid.moveMade(BLACK);
        assertEquals(303_000, rapid.getRemaining(BLACK));
        assertEquals("5 min + 3 s", TimeControl.RAPID.toString());
        assertEquals("No clock", TimeControl.NONE.toString());
    }
}
