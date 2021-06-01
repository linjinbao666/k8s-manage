package ecs.dao;

import ecs.entity.KubernetesNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface KubernetesNodeDao extends JpaRepository<KubernetesNode, Long>, JpaSpecificationExecutor {

    @Query(value = "select * from ecs_kubernetes_node where hostname=?1", nativeQuery = true)
    KubernetesNode findByHostName(String hostname);
}
