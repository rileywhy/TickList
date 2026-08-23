package com.riley.ticklist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SkippedRowRepository extends JpaRepository<SkippedRow, Long> {
    List<SkippedRow> findByImportBatch(ImportBatch importBatch);
    
}
