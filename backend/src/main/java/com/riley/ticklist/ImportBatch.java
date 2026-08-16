package com.riley.ticklist;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;



@Entity
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @Enumerated(EnumType.STRING)
    private SourceApp sourceApp;

    private String originalFilename;
    private Integer successfulRows;
    private Integer failedRows;

    @CreationTimestamp
    private LocalDateTime importedAt;

    public ImportBatch() {
        this.successfulRows = 0;
        this.failedRows = 0;
    }

    public ImportBatch(User user, SourceApp sourceApp, String originalFilename) {
        this();
        this.user = user;
        this.sourceApp = sourceApp;
        this.originalFilename = originalFilename;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SourceApp getSourceApp() {
        return sourceApp;
    }

    public void setSourceApp(SourceApp sourceApp) {
        this.sourceApp = sourceApp;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Integer getSuccessfulRows() {
        return successfulRows;
    }

    public void setSuccessfulRows(Integer successfulRows) {
        this.successfulRows = successfulRows;
    }

    public Integer getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(Integer failedRows) {
        this.failedRows = failedRows;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }



    
}
