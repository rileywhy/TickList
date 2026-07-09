package com.riley.ticklist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class GradeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rawGrade; // "V4", "V4+", "6B", "WI3"

    @Enumerated(EnumType.STRING)
    private GradeSystem gradeSystem;

    @Enumerated(EnumType.STRING)
    private Discipline discipline;

    private Double systemOrder;

    private Double difficultyScore;

    private Double confidence; // optional: 1.0 = confident, 0.5 = fuzzy conversion

    @Column(length = 1000)
    private String note;

    private Boolean active = true;

    public Long getId() {
        return id;
    }

    public String getRawGrade() {
        return rawGrade;
    }

    public void setRawGrade(String rawGrade) {
        this.rawGrade = rawGrade;
    }

    public GradeSystem getGradeSystem() {
        return gradeSystem;
    }

    public void setGradeSystem(GradeSystem gradeSystem) {
        this.gradeSystem = gradeSystem;
    }

    public Discipline getDiscipline() {
        return discipline;
    }

    public void setDiscipline(Discipline discipline) {
        this.discipline = discipline;
    }

    public Double getSystemOrder() {
        return systemOrder;
    }

    public void setSystemOrder(Double systemOrder) {
        this.systemOrder = systemOrder;
    }

    public Double getDifficultyScore() {
        return difficultyScore;
    }

    public void setDifficultyScore(Double difficultyScore) {
        this.difficultyScore = difficultyScore;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
