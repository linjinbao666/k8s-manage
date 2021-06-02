package ama.dao;

import ama.entity.Dockerfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface DockerfileDao extends JpaRepository<Dockerfile, Long>, JpaSpecificationExecutor {
    @Query(value = "select * from dockerfile where file_Name = ?1", nativeQuery = true)
    Dockerfile findByFileName(String fileName);
}
