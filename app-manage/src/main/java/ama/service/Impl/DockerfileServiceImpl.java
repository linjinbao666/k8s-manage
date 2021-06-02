package ama.service.Impl;

import ama.dao.DockerfileDao;
import ama.entity.Dockerfile;
import ama.service.DockerfileService;
import ama.enumlation.CodeEnum;
import ama.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DockerfileServiceImpl implements DockerfileService {

    @Autowired
    DockerfileDao dockerfileDao;

    @Override
    public long count(Map<String, Object> params) {
        Specification<Dockerfile> specification = new Specification<Dockerfile>() {
            @Override
            public Predicate toPredicate(Root<Dockerfile> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaBuilder.and(list.toArray(new Predicate[0]));
            }
        };
        return dockerfileDao.count(specification);
    }

    @Override
    public List<Dockerfile> findAll(Map<String, Object> params) {
        Specification<Dockerfile> specification = new Specification<Dockerfile>() {
            @Override
            public Predicate toPredicate(Root<Dockerfile> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaBuilder.and(list.toArray(new Predicate[0]));
            }
        };

        return dockerfileDao.findAll(specification);
    }

    @Override
    public List<Dockerfile> findAll(Map<String, Object> params, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Specification<Dockerfile> specification = new Specification<Dockerfile>() {
            @Override
            public Predicate toPredicate(Root<Dockerfile> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaBuilder.and(list.toArray(new Predicate[0]));
            }
        };
        Page<Dockerfile> pageObject = dockerfileDao.findAll(specification, pageable);
        List<Dockerfile> dockerfiles = new ArrayList<>();
        pageObject.getContent().forEach(tmp -> { dockerfiles.add(tmp); });
        return dockerfiles;
    }

    @Override
    public Dockerfile findOne(String fileName) {
       return dockerfileDao.findByFileName(fileName);
    }

    @Override
    public ResultVo addOne(Dockerfile dockerfile) {
        Dockerfile byFileName = dockerfileDao.findByFileName(dockerfile.getFileName());
        if (null != byFileName) return ResultVo.renderErr(CodeEnum.ERR).withRemark("文件名称冲突");
        Dockerfile save = dockerfileDao.save(dockerfile);
        return ResultVo.renderOk(save).withRemark("新增成功！");
    }

    @Override
    public ResultVo updateOne(Dockerfile dockerfile) {
        Dockerfile byFileName = dockerfileDao.findByFileName(dockerfile.getFileName());
        if (null == byFileName) return ResultVo.renderErr(CodeEnum.ERR).withRemark("文件不存在");
        dockerfile.setId(byFileName.getId());
        dockerfile.setCreateDate(byFileName.getCreateDate());
        dockerfile.setCreator(byFileName.getCreator());
        Dockerfile save = dockerfileDao.save(dockerfile);
        return ResultVo.renderOk(save).withRemark("修改成功！");
    }

    @Override
    public ResultVo deleteOne(String fileName) {
        Dockerfile byFileName = dockerfileDao.findByFileName(fileName);
        if (null == byFileName) return ResultVo.renderErr(CodeEnum.ERR).withRemark("文件不存在");
        dockerfileDao.delete(byFileName);
        return ResultVo.renderOk(byFileName).withRemark("删除成功！");
    }
}
