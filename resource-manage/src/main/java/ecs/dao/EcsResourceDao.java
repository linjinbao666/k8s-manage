package ecs.dao;

import ecs.entity.EcsResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EcsResourceDao extends JpaRepository<EcsResource, Long>, JpaSpecificationExecutor {
    @Query(value = "select * from ecs_resource where status = ?1", nativeQuery = true)
    EcsResource findByStatus(int status);

    @Query(value = "select * from  ecs_resource where ip =  ?1",nativeQuery = true)
    EcsResource findByIp(String ip);

}
