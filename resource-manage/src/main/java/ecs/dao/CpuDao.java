package ecs.dao;

import ecs.entity.CPU;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CpuDao extends JpaRepository<CPU, Long>, JpaSpecificationExecutor {

    @Query(value = "select * from ecs_uptime where ip = ?1 and date between ?2 and ?3" , nativeQuery = true)
    List<CPU> findByIpAndDate(String ip, String startTime, String endTime);
}
