package com.riley.ticklist;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TickRepository extends JpaRepository<Tick, Long> {
}
