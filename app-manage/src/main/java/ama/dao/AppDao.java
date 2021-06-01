package ama.dao;

import ama.entity.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AppDao extends JpaRepository<App, Long>, JpaSpecificationExecutor {
    @Query(value = "select * from app where app_name = ?1 limit 1", nativeQuery = true)
    App findByAppName(String appName);
    @Query(value = "select * from app where namespace = ?1 and app_name = ?2 limit 1",nativeQuery = true)
    App findOne(String namespace, String appName);
    @Query(value = "select * from app where node_port = ?1", nativeQuery = true)
    App findOneWithNodePort(Integer nodePort);

    @Transactional
    @Modifying
    @Query(value = "delete from app where namespace = ?1 and app_name = ?2", nativeQuery = true)
    void delete(String namespace, String appName);
    @Query(value = "select node_port as nodeport from app", nativeQuery = true)
    List<Integer> findNodePorts();
}
