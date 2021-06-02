package ama.service;

import ama.entity.Repository;
import ama.vo.ResultVo;

import java.util.List;
import java.util.Map;

/**
 * 镜像仓库管理类
 * 仓库的分类： 公有，私有
 */
public interface RepositoryService {

    /**
     * 新增一个仓库
     * @param repository
     * @return
     */
    ResultVo addOne(Repository repository);

    /**
     * 按照条件查找仓库
     * @param params
     * @param pageSize
     * @param pageNum
     * @return
     */
    List<Repository> find(Map<String, Object> params, int pageSize, int pageNum);

    /**
     * 统计数量
     * @param params
     * @return
     */
    long count(Map<String, Object> params);

    /**
     * 更新仓库信息
     * @param repository
     * @return
     */
    ResultVo updateOne(Repository repository);

    /**
     * 删除仓库
     * @return
     */
    ResultVo deleteOne(Long id);
}
