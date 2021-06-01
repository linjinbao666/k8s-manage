package ecs.dao;

import ecs.entity.SubCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SubCenterDao extends JpaRepository<SubCenter, Long>, JpaSpecificationExecutor {

    @Query(value = "select * from ecs_sub_center where center_name=?1", nativeQuery = true)
    SubCenter findByName(String centerName);
}
