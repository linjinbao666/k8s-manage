package monitor.service;

import monitor.entity.AlertHistory;
import monitor.entity.AlertRule;
import monitor.vo.ResultVo;

import java.util.List;
import java.util.Map;

/**
 * 告警规则接口
 */
public interface AlertRuleService {
    /**
     * 查询所有
     * @return
     */
    List<AlertRule> findAll();

    /**
     * 查询所有-带分页
     * @param params
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<AlertRule> findAll(Map<String, Object> params, Integer pageNum, Integer pageSize);

    /**
     * 新增一条
     */
    ResultVo addOne(AlertRule alertRule);

    /**
     * 统计
     * @return
     */

    long count(Map<String, Object> params);

    /**
     * 更新
     * @param id
     * @param target
     * @param quota
     * @param status
     * @return
     */
    ResultVo updateOne(Long id, String target, String quota, Integer status);

    /**
     * 删除
     * @param id
     * @return
     */
    ResultVo deleteOne(Long id);

    /**
     * 根据告警规则查询历史=记录
     * @param ruleId
     * @return
     */
    List<AlertHistory> history(long ruleId, Integer pageNum, Integer pageSize);

    long historyCount(long ruleId);

    /**
     * 查询单个
     * @param id
     * @return
     */
    ResultVo findOne(long id);

}
