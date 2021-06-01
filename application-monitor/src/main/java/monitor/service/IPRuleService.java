package monitor.service;

import monitor.entity.IPRule;
import monitor.vo.ResultVo;

import java.util.List;
import java.util.Map;

/**
 * 告警规则接口
 */
public interface IPRuleService {
    /**
     * 查询所有
     * @return
     */
    List<IPRule> findAll();

    /**
     * 查询所有-带分页
     * @param params
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<IPRule> findAll(Map<String, Object> params, Integer pageNum, Integer pageSize);

    /**
     * 新增一条
     */
    ResultVo addOne(IPRule alertRule);

    /**
     * 统计
     * @return
     */

    long count(Map<String, Object> params);

    /**
     * 更新
     * @param id
     * @param status
     * @return
     */
    ResultVo updateOne(Long id, Integer type,String rule, Integer status);

    /**
     * 删除
     * @param id
     * @return
     */
    ResultVo deleteOne(Long id);
}
