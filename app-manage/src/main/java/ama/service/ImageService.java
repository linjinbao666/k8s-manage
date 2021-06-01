package ama.service;

import ama.entity.DockerImage;
import ama.vo.ResultVo;
import io.minio.errors.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public interface ImageService {

    /**
     * 从仓库同步镜像列表到数据库
     * @implNote 此方法需要进行锁控制，避免频繁请求
     */
    ResultVo syncImages() throws InterruptedException;

    /**
     * 同步指定镜像到数据库
     * @param repository
     * @param imgVersion
     */
    void syncOneImage(String repository, String imgVersion);

    /**
     * 查询总数
     * @return
     */
    long count(Map<String, Object> params);
    /**
     * 获取所有镜像
     * @return
     */
    List<DockerImage> findAll(Map<String, Object> params);
    /**
     * 获取所有镜像-带查询条件
     * @param params
     * @param pageNum 页码
     * @param pageSize 分页大小
     * @return
     */
    List<DockerImage> findAll(Map<String, Object> params, Integer pageNum, Integer pageSize);

    /**
     * 根据镜像id获取单个镜像详情
     * @return
     */
    DockerImage findOne(String imgID);

    /**
     * 导入镜像 tar包
     * @param tarFile
     */
    void loadOneImage(File tarFile);

    /**
     * 导入镜像-从文件流
     * @param tarInputStream
     */
    void loadOneImage(InputStream tarInputStream);

    /**
     * 从文件夹制作镜像，此文件意思是包含docker和资源文件的文件夹
     * 示例：
     * aic
     * ├── aic.war
     * └── Dockerfile
     * @param folder
     */
    ResultVo buildOneImage(String folder, String repository, String imgVersion) throws IOException;

    /**
     * 用户上传zip包制作
     * @param srcFolder 源文件夹路径
     * @param srcFile 源文件路径
     * @param repository
     * @param imgVersion
     * @return
     */
    ResultVo buildZipImage(String srcFolder,String srcFile, String repository, String imgVersion) throws IOException;
    /**
     * 从文件流制作镜像
     *  * 示例：
     *      * ├── aic.war
     *      * └── Dockerfile
     * @// TODO: 2020/9/1
     * @param inputStream
     */
    ResultVo buildOneImage(InputStream inputStream, String repository, String imgVersion) throws InterruptedException;

    /**
     * 支持minio的文件
     * @param
     */
    ResultVo buildOneimage(String bucket, String fileName, String repository, String imgVersion) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException;

    /**
     * 推送镜像到仓库
     * @param repository 镜像名称
     * @param imgVersion 镜像版本
     * @return
     */
    ResultVo pushOneImage(String repository, String imgVersion) throws InterruptedException;

    /**
     * 拉取镜像到本地
     * @return
     */
    ResultVo pullOneImage(String registry,String repository, String imgVersion) throws InterruptedException;
    /**
     * 根据imgName删除镜像
     * @param imgName
     * @return
     */
    ResultVo deleteOneImage(String imgName);

    /**
     * 获取镜像详情
     * @return
     */
    ResultVo getImgDetail(String imgName);

    /**
     * 导出镜像文件
     * @param imgName
     * @return
     */
    InputStream exportImage(String imgName);

    /**
     * 为指定镜像打标签
     * @param repository
     * @param imgVersion
     * @return
     */
    ResultVo tagOneImage(String repository, String imgVersion);

    /**
     * 使用Dockerfile构建镜像
     * @param tempFile
     * @param repository
     * @param imgVersion
     * @return
     */
    ResultVo buildOneImage(File tempFile, String repository, String imgVersion);

    /**
     * 自动检索hub仓库镜像并拉取到本地
     * @return
     */
    ResultVo pullAll() throws IOException;

    /**
     * 拉取镜像
     * @param registry
     * @param repository
     * @param imgVersion
     * @param userName
     * @param passWord
     * @param description
     * @return
     */
    ResultVo pullOneImage(String registry, String repository, String imgVersion, String userName, String passWord, String description) throws InterruptedException;
}
