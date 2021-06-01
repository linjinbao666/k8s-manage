package ama.service.Impl;

import ama.dao.ImageDao;
import ama.dao.RepositoryDao;
import ama.entity.DockerImage;
import ama.entity.Repository;
import ama.service.RepositoryService;
import ama.vo.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

@Service
@Slf4j
public class RepositoryServiceImpl implements RepositoryService {

    @Autowired
    RepositoryDao repositoryDao;
    @Autowired
    ImageDao imageDao;

    @Override
    public ResultVo addOne(Repository repository) {
        Repository one = repositoryDao.findOneByEnName(repository.getEnName());
        if (one != null) {
            return ResultVo.renderErr().withRemark("新增失败，英文名称重复");
        }
        Repository save = repositoryDao.save(repository);
        return ResultVo.renderOk(save).withRemark("操作成功！");
    }

    @Override
    public List<Repository> find(Map<String, Object> params, int pageSize, int pageNum) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Specification<Repository> specification = new Specification<Repository>() {
            @Override
            public Predicate toPredicate(Root<Repository> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaBuilder.and(list.toArray(new Predicate[0]));
            }
        };
        Page<Repository> pageObject = repositoryDao.findAll(specification, pageable);
        List<Repository> repositoryList = new ArrayList<>();
        pageObject.getContent().forEach(tmp -> { repositoryList.add(tmp); });
        return repositoryList;
    }

    @Override
    public long count(Map<String, Object> params) {
        Specification<Repository> specification = new Specification<Repository>() {
            @Override
            public Predicate toPredicate(Root<Repository> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaQuery.where(list.toArray(new Predicate[list.size()])).getRestriction();
            }
        };
        return repositoryDao.count(specification);
    }

    @Override
    public ResultVo updateOne(Repository repository) {
        Optional<Repository> byId = repositoryDao.findById(repository.getId());
        if (!byId.isPresent()) {
            return ResultVo.renderErr().withRemark("更新失败，目标仓库不存在");
        }
        byId.get().setDescription(repository.getDescription());
        byId.get().setType(repository.getType());
        Repository save = repositoryDao.save(byId.get());
        return ResultVo.renderOk(save).withRemark("操作成功！");
    }

    @Override
    public ResultVo deleteOne(Long id) {
        boolean b = repositoryDao.existsById(id);
        if (!b){
           return ResultVo.renderErr(id).withRemark("仓库不存在");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("repositoryId", id);
        List<DockerImage> all = imageDao.findByRepoSitoryID(id);
        if (null != all && all.size() != 0){
            return ResultVo.renderVain().withRemark("仓库不为空，禁止直接删除仓库");
        }
        repositoryDao.deleteById(id);
        return  ResultVo.renderOk(id).withRemark("删除成功");
    }
}
