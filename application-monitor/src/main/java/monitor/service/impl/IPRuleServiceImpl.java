package monitor.service.impl;

import io.fabric8.kubernetes.client.KubernetesClient;
import monitor.dao.IPRuleDao;
import monitor.entity.IPRule;
import monitor.service.IPRuleService;
import monitor.vo.ResultVo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class IPRuleServiceImpl implements IPRuleService {

    @Autowired
    IPRuleDao ipRuleDao;

    @Autowired
    KubernetesClient kubernetesClient;

    @Override
    public List<IPRule> findAll() {
        return ipRuleDao.findAll();
    }

    @Override
    public List<IPRule> findAll(Map<String, Object> params, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("createDate").descending());
        Specification<IPRule> specification = new Specification<IPRule>() {
            @Override
            public Predicate toPredicate(Root<IPRule> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaBuilder.and(list.toArray(new Predicate[0]));
            }
        };
        Page<IPRule> pageObject = ipRuleDao.findAll(specification, pageable);
        List<IPRule> rules = new ArrayList<>();
        pageObject.getContent().forEach(tmp -> { rules.add(tmp); });
        return rules;
    }

    @Override
    public ResultVo addOne(IPRule alertRule) {
        IPRule byName = ipRuleDao.findByName(alertRule.getName());
        if (null != byName) return ResultVo.renderErr().withRemark("名称重复");

        /**
         * 真实到服务器创建规则
         */

//        kubernetesClient.autoscaling()


        IPRule save = ipRuleDao.save(alertRule);
        return ResultVo.renderOk(save).withRemark("新增规则成功");
    }

    @Override
    public long count(Map<String, Object> params) {
        Specification<IPRule> specification = new Specification<IPRule>() {
            @Override
            public Predicate toPredicate(Root<IPRule> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaBuilder.and(list.toArray(new Predicate[0]));
            }
        };
        return ipRuleDao.count(specification);
    }

    @Override
    public ResultVo updateOne(Long id, Integer type,String rule, Integer status) {
        boolean b = ipRuleDao.existsById(id);
        if (!b) return ResultVo.renderErr().withRemark("目标记录不存在，更新失败");
        IPRule ipRule = ipRuleDao.findById(id).get();
        ipRule.setType(type);
        ipRule.setUpdateDate(new Date());
        ipRule.setStatus(status);
        ipRule.setRule(rule);
        IPRule save = ipRuleDao.save(ipRule);
        return ResultVo.renderOk(save).withRemark("更细成功！");
    }

    @Override
    public ResultVo deleteOne(Long id) {
        boolean b = ipRuleDao.existsById(id);
        if (!b) return ResultVo.renderErr().withRemark("目标记录不存在，删除失败");
        ipRuleDao.deleteById(id);
        return ResultVo.renderOk().withRemark("删除成功");

    }
}
