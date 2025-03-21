package com.mapdrop.models;

import jakarta.persistence.*;
import lombok.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "games")
public class Game implements Comparable<Game> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name="points")
    private double points;

    @Column(name="location")
    private String location;

    @Column(name="longitude_guess")
    private double longitude_guess;

    @Column(name="latitude_guess")
    private double latitude_guess;

    @Column(name="longitude_real")
    private double longitude_real;

    @Column(name="latitude_real")
    private double latitude_real;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    public Game(double points, String location) {
        this.points = points;
        this.location = location;
    }

    public Game(int id) {this.id = id; }

    @Override
    public int compareTo(Game game) {
        if (getCreatedAt() == null || game.getCreatedAt() == null) {
            return 0;
        }
        return getCreatedAt().compareTo(game.getCreatedAt());
    }
}
