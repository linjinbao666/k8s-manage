package ecs.dao;

import ecs.entity.Pvc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PvcDao extends JpaRepository<Pvc, Long>, JpaSpecificationExecutor {

    @Query(value = "select * from ecs_pvc where namespace = ?1 and pvc_name = ?2", nativeQuery = true)
    Pvc findByPvcName(String namespace, String pvcName);
}
