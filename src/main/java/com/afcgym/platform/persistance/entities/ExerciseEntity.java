package com.afcgym.platform.persistance.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exercise")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExerciseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false, length = 150)
    private String name;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false)
    private int sets;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false, length = 30)
    private String reps;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "rest_seconds")
    private Integer restSeconds;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "video_file", length = 60)
    private String videoFile;

    public void updateDetails(int sortOrder, String name, int sets, String reps, Integer restSeconds) {
        setSortOrder(sortOrder);
        setName(name);
        setSets(sets);
        setReps(reps);
        setRestSeconds(restSeconds);
    }

    public String useVideoUrl(String videoUrl) {
        String previousFile = videoFile;
        setVideoFile(null);
        setVideoUrl(videoUrl);
        return previousFile;
    }

    public String useVideoFile(String videoFile) {
        String previousFile = this.videoFile;
        setVideoUrl(null);
        setVideoFile(videoFile);
        return previousFile;
    }
}
