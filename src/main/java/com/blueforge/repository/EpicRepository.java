package com.blueforge.repository;

import com.blueforge.entity.Epic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpicRepository extends JpaRepository<Epic, Long> {}
