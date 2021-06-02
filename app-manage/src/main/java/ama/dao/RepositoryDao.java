package ama.dao;

import ama.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface RepositoryDao extends JpaRepository<Repository, Long>, JpaSpecificationExecutor {

    /**
     * 根据英文名称查询
     * @param enName
     * @return
     */
    @Query(value = "select * from repository where en_name = ?1 limit 1", nativeQuery = true)
    Repository findOneByEnName(String enName);
}
