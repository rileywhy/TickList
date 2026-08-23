package com.riley.ticklist;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;



@Entity
public class SkippedRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(columnDefinition = "TEXT")
    private String reason;
    @Column(columnDefinition = "TEXT")
    private String rawRow;
    @ManyToOne(optional = false)
    private ImportBatch importBatch;
    private long recordNumber;


    protected SkippedRow() {
        // Default constructor for JPA
    }

    public SkippedRow(long recordNumber, String reason, String rawRow, ImportBatch importBatch) {
        this.recordNumber = recordNumber;
        this.reason = reason;
        this.rawRow = rawRow;
        this.importBatch = importBatch;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public String getReason() {
        return reason;
    }

    public String getRawRow() {
        return rawRow;
    }

    public ImportBatch getImportBatch() {
        return importBatch;
    }

    public long getRecordNumber() {
        return recordNumber;
    }


}
