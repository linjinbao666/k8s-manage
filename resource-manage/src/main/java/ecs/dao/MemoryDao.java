package ecs.dao;

import ecs.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemoryDao extends JpaRepository<Memory, Long>, JpaSpecificationExecutor {

    @Query(value = "select * from ecs_memory where ip = ?1 and date between ?2 and ?3", nativeQuery = true)
    List<Memory> findByIpAndDate(String ip, String startTime, String endTime);
}
