package ecs.dao;

import ecs.entity.Pv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PvDao extends JpaRepository<Pv, Long>, JpaSpecificationExecutor {
    @Query(value = "select * from ecs_pv where ecs_pv.pv_name =?1 ", nativeQuery = true)
    Pv findByPvName(String pvName);
}
