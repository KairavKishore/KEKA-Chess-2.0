package chess.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The mini-game that ends a hack: buttons numbered from 1 upwards sit in a
 * shuffled order and must be pressed in counting order. A wrong press
 * starts the count again and shuffles them. Kept apart from JavaFX so it
 * can be tested without a window.
 */
final class HackPuzzle {

    private final Random random;
    private final List<Integer> numbers = new ArrayList<>();

    /** The number that has to be pressed next. */
    private int next = 1;

    /**
     * @param size how many buttons there are
     */
    HackPuzzle(int size, Random random) {
        this.random = random;
        for (int number = 1; number <= size; number++) {
            numbers.add(number);
        }
        Collections.shuffle(numbers, random);
    }

    int getSize() {
        return numbers.size();
    }

    /** The number shown on the button in a slot, counting slots from 0. */
    int numberAt(int slot) {
        return numbers.get(slot);
    }

    /** The number that has to be pressed next. */
    int getNext() {
        return next;
    }

    /** How many have been pressed correctly in a row. */
    int getProgress() {
        return next - 1;
    }

    boolean isSolved() {
        return next > numbers.size();
    }

    /**
     * Presses the button in a slot. The right one moves the count on. A
     * wrong one, or one already pressed, starts the count again and
     * shuffles the buttons.
     *
     * @return true if it was the right button
     */
    boolean press(int slot) {
        if (isSolved()) {
            return false;
        }
        if (numbers.get(slot) == next) {
            next++;
            return true;
        }
        next = 1;
        Collections.shuffle(numbers, random);
        return false;
    }
}
