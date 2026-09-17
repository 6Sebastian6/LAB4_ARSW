package edu.eci.arsw.blueprints.persistence;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.entity.BlueprintEntity;
import edu.eci.arsw.blueprints.persistence.entity.BlueprintJpaRepository;
import edu.eci.arsw.blueprints.persistence.entity.PointEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Primary
@Transactional
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final BlueprintJpaRepository repository;

    public PostgresBlueprintPersistence(BlueprintJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        if (repository.existsByAuthorAndName(bp.getAuthor(), bp.getName())) {
            throw new BlueprintPersistenceException(
                    "Blueprint already exists: " + bp.getAuthor() + ":" + bp.getName());
        }
        repository.save(toEntity(bp));
    }

    @Override
    @Transactional(readOnly = true)
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        return toDomain(findEntity(author, name));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        List<BlueprintEntity> entities = repository.findByAuthor(author);
        if (entities.isEmpty()) {
            throw new BlueprintNotFoundException("No blueprints for author: " + author);
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Blueprint> getAllBlueprints() {
        return repository.findAll().stream().map(this::toDomain).collect(Collectors.toSet());
    }

    @Override
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        findEntity(author, name).addPoint(new PointEntity(x, y));
    }

    private BlueprintEntity findEntity(String author, String name) throws BlueprintNotFoundException {
        return repository.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException(
                        "Blueprint not found: %s/%s".formatted(author, name)));
    }

    private BlueprintEntity toEntity(Blueprint bp) {
        BlueprintEntity entity = new BlueprintEntity(bp.getAuthor(), bp.getName());
        bp.getPoints().forEach(p -> entity.addPoint(new PointEntity(p.x(), p.y())));
        return entity;
    }

    private Blueprint toDomain(BlueprintEntity entity) {
        List<Point> points = entity.getPoints().stream()
                .map(p -> new Point(p.getX(), p.getY()))
                .toList();
        return new Blueprint(entity.getAuthor(), entity.getName(), points);
    }
}
