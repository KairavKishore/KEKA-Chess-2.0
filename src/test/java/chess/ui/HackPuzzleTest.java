package chess.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HackPuzzleTest {

    private final HackPuzzle puzzle = new HackPuzzle(6, new Random(3));

    private int slotOf(int number) {
        for (int slot = 0; slot < puzzle.getSize(); slot++) {
            if (puzzle.numberAt(slot) == number) {
                return slot;
            }
        }
        throw new AssertionError("No button shows " + number);
    }

    @Test
    void everyNumberAppearsOnceInAMuddle() {
        Set<Integer> seen = new HashSet<>();
        boolean inOrder = true;
        for (int slot = 0; slot < 6; slot++) {
            seen.add(puzzle.numberAt(slot));
            inOrder &= puzzle.numberAt(slot) == slot + 1;
        }
        assertEquals(Set.of(1, 2, 3, 4, 5, 6), seen);
        assertFalse(inOrder, "the buttons should not start in counting order");
        assertEquals(1, puzzle.getNext());
        assertEquals(0, puzzle.getProgress());
        assertFalse(puzzle.isSolved());
    }

    @Test
    void pressingInOrderSolvesIt() {
        for (int number = 1; number <= 6; number++) {
            assertFalse(puzzle.isSolved());
            assertTrue(puzzle.press(slotOf(number)));
            assertEquals(number, puzzle.getProgress());
        }
        assertTrue(puzzle.isSolved());
    }

    @Test
    void wrongPressStartsAgainAndMuddlesTheButtons() {
        puzzle.press(slotOf(1));
        puzzle.press(slotOf(2));
        int[] before = new int[6];
        for (int slot = 0; slot < 6; slot++) {
            before[slot] = puzzle.numberAt(slot);
        }

        assertFalse(puzzle.press(slotOf(5)));

        assertEquals(1, puzzle.getNext());
        assertEquals(0, puzzle.getProgress());
        boolean moved = false;
        for (int slot = 0; slot < 6; slot++) {
            moved |= before[slot] != puzzle.numberAt(slot);
        }
        assertTrue(moved, "the buttons should have been shuffled");
    }

    @Test
    void pressingANumberAlreadyDoneIsAlsoWrong() {
        puzzle.press(slotOf(1));
        assertFalse(puzzle.press(slotOf(1)));
        assertEquals(1, puzzle.getNext());
    }

    @Test
    void canStillBeSolvedAfterMistakes() {
        puzzle.press(slotOf(3));
        puzzle.press(slotOf(1));
        puzzle.press(slotOf(6));
        for (int number = 1; number <= 6; number++) {
            puzzle.press(slotOf(number));
        }
        assertTrue(puzzle.isSolved());
        assertFalse(puzzle.press(0));   // nothing more to press
        assertTrue(puzzle.isSolved());
    }
}
