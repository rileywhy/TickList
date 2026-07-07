package com.riley.ticklist;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TickRepository extends JpaRepository<Tick, Long> {
    List<Tick> findByUser(User user);
    Optional<Tick> findByIdAndUser(Long id, User user);

}
