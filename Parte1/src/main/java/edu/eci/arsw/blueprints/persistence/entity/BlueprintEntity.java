package edu.eci.arsw.blueprints.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blueprints", uniqueConstraints = @UniqueConstraint(columnNames = {"author", "name"}))
public class BlueprintEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "blueprint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "position")
    private List<PointEntity> points = new ArrayList<>();

    protected BlueprintEntity() { }

    public BlueprintEntity(String author, String name) {
        this.author = author;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getAuthor() { return author; }
    public String getName() { return name; }
    public List<PointEntity> getPoints() { return points; }

    public void addPoint(PointEntity point) {
        point.setBlueprint(this);
        points.add(point);
    }
}
