package ama.service;

import ama.entity.Dockerfile;
import ama.vo.ResultVo;

import java.util.List;
import java.util.Map;

/**
 * Dockerfile 管理
 */
public interface DockerfileService {

    /**
     * 查询总数
     * @return
     */
    long count(Map<String, Object> params);

    /**
     * 查询所有
     * @return
     */
    List<Dockerfile> findAll(Map<String, Object> params);

    /**
     * 查询分页
     * @param params
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<Dockerfile> findAll(Map<String, Object> params, Integer pageNum, Integer pageSize);

    /**
     * 根据文件名查找一个
     * @param fileName
     * @return
     */
    Dockerfile findOne(String fileName);

    /**
     * 新增一个
     * @param dockerfile
     */
    ResultVo addOne(Dockerfile dockerfile);

    /**
     * 修改一个
     * @param dockerfile
     * @return
     */
    ResultVo updateOne(Dockerfile dockerfile);

    /**
     * 删除一个
     * @param fileName
     * @return
     */
    ResultVo deleteOne(String fileName);
}
