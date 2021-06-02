package monitor.service.impl;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import monitor.dao.AlertHistoryDao;
import monitor.dao.AlertRuleDao;
import monitor.entity.AlertHistory;
import monitor.entity.AlertRule;
import monitor.enumlation.CodeEnum;
import monitor.service.AlertRuleService;
import monitor.util.Pinyin4jUtil;
import monitor.util.PrometheusExprUtil;
import monitor.vo.ResultVo;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
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
import java.util.*;

/**
 * 告警接口实现
 */
@Service
public class AlertRuleServiceImpl implements AlertRuleService {

    @Autowired
    AlertRuleDao alertRuleDao;

    @Override
    public List<AlertRule> findAll() {
        List<AlertRule> all = alertRuleDao.findAll();
        return all;
    }

    @Override
    public List<AlertRule> findAll(Map<String, Object> params, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("createDate").descending());
        Specification<AlertRule> specification = new Specification<AlertRule>() {
            @Override
            public Predicate toPredicate(Root<AlertRule> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaBuilder.and(list.toArray(new Predicate[0]));
            }
        };
        Page<AlertRule> pageObject = alertRuleDao.findAll(specification, pageable);
        List<AlertRule> rules = new ArrayList<>();
        pageObject.getContent().forEach(tmp -> { rules.add(tmp); });
        return rules;
    }

    @Autowired
    KubernetesClient kubernetesClient;

    @Override
    public ResultVo addOne(AlertRule alertRule) {
        AlertRule byName = alertRuleDao.findByName(alertRule.getAlertName());
        if (null != byName) return ResultVo.renderErr().withRemark("重复");
        String enName = Pinyin4jUtil.converterToFirstSpell(alertRule.getAlertName());
        alertRule.setEnName(enName);
        if(0 == alertRule.getStatus()){
            AlertRule save = alertRuleDao.save(alertRule);
            return ResultVo.renderOk().withRemark("新增成功,告警规则未开启");
        }

        String rule = "apiVersion: monitoring.coreos.com/v1\n" +
                "kind: PrometheusRule\n" +
                "metadata:\n" +
                "  name: "+ enName +"\n" +
                "  namespace: monitoring\n" +
                "  labels:\n" +
                "    prometheus: k8s\n"+
                "    role: alert-rules\n"+
                "spec:\n" +
                "  groups:\n" +
                "  - name: "+ enName +"\n" +
                "    rules:\n" +
                "      - alert: "+enName+"\n" +
                "        expr: >-\n" +
                "          "+PrometheusExprUtil.getExpr(alertRule.getTarget(), Double.valueOf(alertRule.getQuota()),alertRule.getAppName())+"\n" +
                "        for: 30s\n" +
                "        labels:\n" +
                "          page: monitoring\n" +
                "          team: monitoring\n" +
                "        annotations:\n" +
                "          summary: "+ alertRule.getAlertDesc() +"\n" +
                "          description: |\n" +
                "            Check failing services";

        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                .withGroup("monitoring.coreos.com")
                .withPlural("prometheusrules")
                .withScope("Namespaced")
                .withVersion("v1")
                .build();
        try {
            kubernetesClient.customResource(crdContext).create("monitoring", rule);
        }catch (Exception e){
            e.printStackTrace();
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("操作出错："+e.getMessage());
        }

        AlertRule save = alertRuleDao.save(alertRule);
        return ResultVo.renderOk(save);
    }

    @Override
    public long count(Map<String, Object> params) {
        Specification<AlertRule> specification = new Specification<AlertRule>() {
            @Override
            public Predicate toPredicate(Root<AlertRule> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaBuilder.and(list.toArray(new Predicate[0]));
            }
        };
        return alertRuleDao.count(specification);
    }

    @Override
    public ResultVo updateOne(Long id, String target, String quota, Integer status) {
        boolean b = alertRuleDao.existsById(id);
        if (!b) return ResultVo.renderErr().withRemark("目标记录不存在");
        AlertRule byId = alertRuleDao.findById(id).get();
        byId.setUpdateDate(new Date());
        byId.setTarget(target);
        byId.setQuota(quota);
        byId.setStatus(status);
        String rule = "apiVersion: monitoring.coreos.com/v1\n" +
                "kind: PrometheusRule\n" +
                "metadata:\n" +
                "  name: "+ byId.getEnName() +"\n" +
                "  namespace: monitoring\n" +
                "  labels:\n" +
                "    prometheus: k8s\n"+
                "    role: alert-rules\n"+
                "spec:\n" +
                "  groups:\n" +
                "  - name: "+ byId.getEnName() +"\n" +
                "    rules:\n" +
                "      - alert: Prometheus scraping errors\n" +
                "        expr: >-\n" +
                "          "+PrometheusExprUtil.getExpr(byId.getTarget(), Double.valueOf(byId.getQuota()),byId.getAppName())+"\n" +
                "        for: 15m\n" +
                "        labels:\n" +
                "          page: monitoring\n" +
                "          team: monitoring\n" +
                "        annotations:\n" +
                "          summary: "+ byId.getAlertDesc() +"\n" +
                "          description: |\n" +
                "            Check failing services";
        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                .withGroup("monitoring.coreos.com")
                .withPlural("prometheusrules")
                .withScope("Namespaced")
                .withVersion("v1")
                .build();
        try {
            if (status == 1){
                kubernetesClient.customResource(crdContext)
                        .createOrReplace("monitoring",rule);
            }else {
                Object monitoring = kubernetesClient.customResource(crdContext).list("monitoring")
                        .get(byId.getAlertName());
                if (null != monitoring){
                    kubernetesClient.customResource(crdContext).delete("monitoring",byId.getAlertName());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        AlertRule save = alertRuleDao.save(byId);
        return ResultVo.renderOk(save).withRemark("修改成功！");
    }

    @Override
    public ResultVo deleteOne(Long id) {
        boolean b = alertRuleDao.existsById(id);
        if (!b) return ResultVo.renderErr().withRemark("目标记录不存在");
        AlertRule alertRule = alertRuleDao.findById(id).get();
        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                .withGroup("monitoring.coreos.com")
                .withPlural("prometheusrules")
                .withScope("Namespaced")
                .withVersion("v1")
                .build();
        try {
//            Map<String, Object> monitoring = kubernetesClient.customResource(crdContext)
//                    .get("monitoring", alertRule.getAlertName());
            Object monitoring = kubernetesClient.customResource(crdContext).list("monitoring").get(alertRule.getEnName());

            if (null != monitoring){
                kubernetesClient.customResource(crdContext).delete("monitoring",alertRule.getEnName());
            }

        }catch (Exception e){
            e.printStackTrace();
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("操作出错，可能由于服务器数据不一致");
        }

        alertRuleDao.delete(alertRule);
        return ResultVo.renderOk().withRemark("删除成功");
    }

    @Autowired
    AlertHistoryDao alertHistoryDao;

    @Override
    public List<AlertHistory> history(long ruleId, Integer pageNum, Integer pageSize) {

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("createDate").descending());

        Specification spec = new Specification<AlertRule>() {
            @Override
            public Predicate toPredicate(Root<AlertRule> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                list.add(criteriaBuilder.equal(root.get("ruleId").as(Long.class), ruleId));
                return criteriaBuilder.and(list.toArray(new Predicate[0]));
            }
        };
        Page<AlertHistory> pageObject = alertHistoryDao.findAll(spec,pageable);

        List<AlertHistory> histories = new ArrayList<>();
        pageObject.getContent().forEach(tmp -> { histories.add(tmp); });

        return histories;
    }

    public long historyCount(long ruleId){
        Specification spec = new Specification<AlertRule>() {
            @Override
            public Predicate toPredicate(Root<AlertRule> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                list.add(criteriaBuilder.equal(root.get("ruleId").as(Long.class), ruleId));
                return criteriaBuilder.and(list.toArray(new Predicate[0]));
            }
        };
        return alertHistoryDao.count(spec);
    }

    @Override
    public ResultVo findOne(long id) {
        boolean b = alertRuleDao.existsById(id);
        if (!b) return ResultVo.renderErr(CodeEnum.ERR).withRemark("当前记录不存在");
        AlertRule alertRule = alertRuleDao.findById(id).get();
        return ResultVo.renderOk(alertRule);
    }
}
