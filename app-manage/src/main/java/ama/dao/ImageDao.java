package ama.dao;

import ama.entity.DockerImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ImageDao extends JpaRepository<DockerImage, Long>, JpaSpecificationExecutor {
    @Query(value = "select * from dockerimage where img_name = ?1 limit 1", nativeQuery = true)
    DockerImage findbyImgName(String imgName);

    @Query(value = "select * from dockerimage where imgid = ?1 limit 1", nativeQuery = true)
    DockerImage findByImgID(String imgID);

    @Query(value = "select * from dockerimage where  repository_id = ?1", nativeQuery = true)
    List<DockerImage> findByRepoSitoryID(Long id);
}
