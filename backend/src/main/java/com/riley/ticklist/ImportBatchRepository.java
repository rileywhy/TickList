package com.riley.ticklist;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {
    List<ImportBatch> findByUser(User user);
    Optional<ImportBatch> findByIdAndUser(Long id, User user);

}
