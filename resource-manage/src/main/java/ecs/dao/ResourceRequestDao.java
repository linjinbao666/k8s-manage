package ecs.dao;

import ecs.entity.ResourceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ResourceRequestDao extends JpaRepository<ResourceRequest, Long>, JpaSpecificationExecutor {
    @Query(value = "select * from ecs_resource_request where namespace = ?1 limit 1", nativeQuery = true)
    ResourceRequest findByNamespace(String namespace);

    @Modifying
    @Transactional
    @Query(value = "delete from ecs_resource_request where namespace = ?1", nativeQuery = true)
    void deleteByNamespace(String namespace);

    @Query(value = "select * from ecs_resource_request where sub_id = ?1 limit 1", nativeQuery = true)
    ResourceRequest findBySubCenter(Long id);

    @Query(value = "select reg_id from ecs_resource_request where reg_id = ?1", nativeQuery = true)
    List<ResourceRequest> findByRegId(Long unit);

    @Query(value = "select * from ecs_resource_request where reg_id = ?1", nativeQuery = true)
    List<ResourceRequest> findAllByRegId(Long unit);

    @Query(value = "select * from ecs_resource_request where reg_id = ?1 or namespace = ?2 limit 1", nativeQuery = true)
    ResourceRequest findByOrgOrNamespace(Long regId, String namespace);

    @Query(value = "select * from ecs_resource_request where reg_id = ?1 and namespace = ?2 limit 1", nativeQuery = true)
    ResourceRequest findByOrgAndNamespace(Long regId, String namespace);

}
