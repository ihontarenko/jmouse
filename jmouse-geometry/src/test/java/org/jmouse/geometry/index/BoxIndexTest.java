package org.jmouse.geometry.index;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxIndexTest {

    private static List<Box> scattered(int count, long seed) {
        Random random = new Random(seed);
        List<Box> boxes = new ArrayList<>();
        for (int number = 0; number < count; number++) {
            double x = random.nextDouble() * 100;
            double y = random.nextDouble() * 100;
            boxes.add(new Box(x, y, x + random.nextDouble() * 3, y + random.nextDouble() * 3));
        }
        return boxes;
    }

    /** What truly comes near a box, by asking every box. */
    private static int[] truly(List<Box> boxes, Box asked) {
        return IntStream.range(0, boxes.size()).filter(number -> boxes.get(number).within(asked, 0)).toArray();
    }

    /** Every number that truly comes near is answered, in ascending order. */
    private static void answersEveryTrulyNear(BoxIndex index, List<Box> boxes) {
        Random random = new Random(7);
        for (int ask = 0; ask < 500; ask++) {
            Box asked = Box.around(random.nextDouble() * 100, random.nextDouble() * 100, random.nextDouble() * 5);
            int[] answered = index.near(asked);
            for (int at = 1; at < answered.length; at++) {
                assertTrue(answered[at - 1] < answered[at], "ascending, each once");
            }
            for (int number : truly(boxes, asked)) {
                assertTrue(IntStream.of(answered).anyMatch(found -> found == number), "box " + number + " missed");
            }
            assertEquals(answered.length, index.nearSet(asked).cardinality());
        }
    }

    @Test
    void treeAnswersExactlyWhatComesNear() {
        List<Box> boxes = scattered(2000, 1);
        BoxIndex tree = BoxTree.of(boxes);
        answersEveryTrulyNear(tree, boxes);
        Box asked = Box.around(50, 50, 4);
        assertArrayEquals(truly(boxes, asked), tree.near(asked));
    }

    @Test
    void treeOfAnyNodeSizeAnswersExactlyWhatComesNear() {
        for (int nodeSize : new int[] {2, 3, 16, 64}) {
            for (int count : new int[] {1, 2, 9, 10, 11, 101, 3000}) {
                List<Box> boxes = scattered(count, count * 31L + nodeSize);
                BoxIndex tree = BoxTree.of(boxes, nodeSize);
                Random random = new Random(count);
                for (int ask = 0; ask < 200; ask++) {
                    Box asked = Box.around(random.nextDouble() * 100, random.nextDouble() * 100, random.nextDouble() * 8);
                    assertArrayEquals(truly(boxes, asked), tree.near(asked), count + " boxes in nodes of " + nodeSize);
                }
            }
        }
    }

    @Test
    void gridAnswersEveryThingNear() {
        List<Box> boxes = scattered(2000, 2);
        answersEveryTrulyNear(BoxGrid.of(boxes, 1.5, 1 << 20), boxes);
        answersEveryTrulyNear(BoxGrid.even(boxes), boxes);
    }

    @Test
    void gridWidensItsCellsPastTheMostItMayTake() {
        List<Box> boxes = scattered(500, 3);
        answersEveryTrulyNear(BoxGrid.of(boxes, 0.01, 64), boxes);
    }

    @Test
    void anyNearStopsAtTheFirstYes() {
        List<Box> boxes = List.of(Box.point(0, 0), Box.point(1, 0), Box.point(2, 0));
        int[] asked = {0};
        assertTrue(BoxTree.of(boxes).anyNear(Box.around(1, 0, 5), number -> {
            asked[0]++;
            return number == 1;
        }));
        assertEquals(2, asked[0]);
    }

    @Test
    void noBoxesAnswerNothing() {
        assertEquals(0, BoxTree.of(List.of()).near(Box.around(0, 0, 1)).length);
        assertEquals(0, BoxGrid.even(List.of()).near(Box.around(0, 0, 1)).length);
    }
}
