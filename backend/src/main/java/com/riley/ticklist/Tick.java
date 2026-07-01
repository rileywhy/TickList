package com.riley.ticklist;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Tick {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String climbName;

    private String climbId;
    @Enumerated(EnumType.STRING)
    private Discipline discipline = Discipline.UNKNOWN;

    @Enumerated(EnumType.STRING)
    private TickType tickType = TickType.UNKNOWN;

    @ManyToOne
    private User user;

    private String location;

    private String grade;

    private String rawGrade;

    private Double gradeValue;
    
    private String personalGrade;
    private Double stars;
    
    private Double climbHeight;


    @Enumerated(EnumType.STRING)
    private GradeSystem gradeSystem = GradeSystem.UNKNOWN;
    
    @ManyToOne
    private GradeMapping gradeMapping;
    
    @Enumerated(EnumType.STRING)
    private SourceApp sourceApp = SourceApp.UNKNOWN;
    
    private String externalId;
    
    private String sourceUrl;
    
    private String style;
    
    @Enumerated(EnumType.STRING)
    private RopeStyle ropeStyle = RopeStyle.UNKNOWN;
    
    private LocalDate tickDate;
    
    private Integer attempts;
    
    private Integer pitches;

    private Double userStars;


    @Column(length = 2000)
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Tick() {
    }

    public Long getId() {
        return id;
    }

    public String getClimbName() {
        return climbName;
    }

    public void setClimbName(String climbName) {
        this.climbName = climbName;
    }

    public String getClimbId() {
        return climbId;
    }

    public void setClimbId(String climbId) {
        this.climbId = climbId;
    }

    public TickType getTickType() {
        return tickType;
    }

    public void setTickType(TickType tickType) {
        this.tickType = tickType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Double getGradeValue() {
        return gradeValue;
    }

    public void setGradeValue(Double gradeValue) {
        this.gradeValue = gradeValue;
    }


    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
        if (this.rawGrade == null) {
            this.rawGrade = grade;
        }
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

    public Integer getPitches() {
        return pitches;
    }

    public void setPitches(Integer pitches) {
        this.pitches = pitches;
    }

    public void setGradeSystem(GradeSystem gradeSystem) {
        this.gradeSystem = gradeSystem;
    }

    public GradeMapping getGradeMapping() {
        return gradeMapping;
    }

    public void setGradeMapping(GradeMapping gradeMapping) {
        this.gradeMapping = gradeMapping;
    }

    public SourceApp getSourceApp() {
        return sourceApp;
    }

    public void setSourceApp(SourceApp sourceApp) {
        this.sourceApp = sourceApp;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public RopeStyle getRopeStyle() {
        return ropeStyle;
    }

    public void setRopeStyle(RopeStyle ropeStyle) {
        this.ropeStyle = ropeStyle;
    }


    public LocalDate getTickDate() {
        return tickDate;
    }

    public void setTickDate(LocalDate tickDate) {
        this.tickDate = tickDate;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setStars(Double stars) {
        this.stars = stars;
    }

    public Double getStars() {
        return stars;
    }

    public Double getUserStars() {
        return userStars;
    }

    public void setUserStars(Double userStars) {
        this.userStars = userStars;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Discipline getDiscipline() {
        return discipline;
    }

    public void setDiscipline(Discipline discipline) {
        this.discipline = discipline;
    }

    public String getPersonalGrade() {
        return personalGrade;
    }

    public void setPersonalGrade(String personalGrade) {
        this.personalGrade = personalGrade;
    }

    public Double getClimbHeight() {
        return this.climbHeight;
    }

    public void setClimbHeight(Double climbHeight) {
        this.climbHeight = climbHeight;
    }








}
