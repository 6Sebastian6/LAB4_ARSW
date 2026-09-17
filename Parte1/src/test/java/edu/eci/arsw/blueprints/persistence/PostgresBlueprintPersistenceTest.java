package edu.eci.arsw.blueprints.persistence;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(PostgresBlueprintPersistence.class)
class PostgresBlueprintPersistenceTest {

    @Autowired
    private PostgresBlueprintPersistence persistence;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsBackBlueprintPreservingPointOrder() throws Exception {
        List<Point> points = List.of(new Point(0, 0), new Point(10, 0), new Point(10, 10));
        persistence.saveBlueprint(new Blueprint("john", "house", points));
        reload();

        Blueprint stored = persistence.getBlueprint("john", "house");

        assertEquals("john", stored.getAuthor());
        assertEquals(points, stored.getPoints());
    }

    @Test
    void rejectsDuplicateAuthorAndName() throws Exception {
        persistence.saveBlueprint(new Blueprint("john", "house", List.of()));

        assertThrows(BlueprintPersistenceException.class,
                () -> persistence.saveBlueprint(new Blueprint("john", "house", List.of())));
    }

    @Test
    void missingBlueprintThrowsNotFound() {
        assertThrows(BlueprintNotFoundException.class, () -> persistence.getBlueprint("john", "none"));
        assertThrows(BlueprintNotFoundException.class, () -> persistence.getBlueprintsByAuthor("nobody"));
    }

    @Test
    void groupsBlueprintsByAuthor() throws Exception {
        persistence.saveBlueprint(new Blueprint("john", "house", List.of()));
        persistence.saveBlueprint(new Blueprint("john", "garage", List.of()));
        persistence.saveBlueprint(new Blueprint("jane", "garden", List.of()));
        reload();

        Set<Blueprint> johns = persistence.getBlueprintsByAuthor("john");

        assertEquals(2, johns.size());
        assertTrue(johns.stream().allMatch(bp -> bp.getAuthor().equals("john")));
        assertEquals(3, persistence.getAllBlueprints().size());
    }

    @Test
    void appendsPointToExistingBlueprint() throws Exception {
        persistence.saveBlueprint(new Blueprint("john", "house", List.of(new Point(0, 0))));
        reload();

        persistence.addPoint("john", "house", 5, 5);
        reload();

        assertEquals(List.of(new Point(0, 0), new Point(5, 5)),
                persistence.getBlueprint("john", "house").getPoints());
    }

    private void reload() {
        entityManager.flush();
        entityManager.clear();
    }
}
