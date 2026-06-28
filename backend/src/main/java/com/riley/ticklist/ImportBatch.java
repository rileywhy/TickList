package com.riley.ticklist;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;


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

    private Integer totalRows;
    private Integer successfulRows;
    private Integer failedRows;

    @CreationTimestamp
    private LocalDateTime importedAt;
}
