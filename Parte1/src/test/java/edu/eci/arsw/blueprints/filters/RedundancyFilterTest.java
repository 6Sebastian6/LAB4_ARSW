package edu.eci.arsw.blueprints.filters;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedundancyFilterTest {

    private final RedundancyFilter filter = new RedundancyFilter();

    @Test
    void removesConsecutiveDuplicatesOnly() {
        Blueprint bp = new Blueprint("john", "house",
                List.of(new Point(0, 0), new Point(0, 0), new Point(1, 1), new Point(0, 0)));

        Blueprint filtered = filter.apply(bp);

        assertEquals(List.of(new Point(0, 0), new Point(1, 1), new Point(0, 0)), filtered.getPoints());
    }

    @Test
    void keepsBlueprintWithoutDuplicatesUnchanged() {
        List<Point> points = List.of(new Point(0, 0), new Point(1, 1));

        Blueprint filtered = filter.apply(new Blueprint("john", "house", points));

        assertEquals(points, filtered.getPoints());
    }
}
