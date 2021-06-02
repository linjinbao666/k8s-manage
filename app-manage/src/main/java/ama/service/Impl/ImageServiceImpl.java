package ama.service.Impl;

import ama.dao.RepositoryDao;
import ama.entity.Repository;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import ama.dao.ImageDao;
import ama.dao.DockerfileDao;
import ama.entity.DockerImage;
import ama.entity.Dockerfile;
import ama.enumlation.UnzipEnum;
import ama.service.ImageService;
import ama.util.MyUtil;
import ama.util.TimeUtil;
import ama.enumlation.CodeEnum;
import ama.vo.DockerImageVo;
import ama.vo.ResultVo;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthResponse;
import com.github.dockerjava.api.model.Image;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.directory.api.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ImageServiceImpl implements ImageService {
    @Autowired
    ImageDao imageDao;
    @Autowired
    DockerClient dockerClient;
    @Autowired
    MinioClient minioClient;

    @Autowired
    RepositoryDao repositoryDao;

    @Value("${docker-java.registerUrl}")
    String library;

    @Override
    public ResultVo syncImages() {
        List<DockerImage> dockerImages = new ArrayList<>();
        List<Image> images = dockerClient.listImagesCmd().exec();
        DateTimeFormatter dtft = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        images.forEach(image -> {
            Long created = image.getCreated();
            String imgId = image.getId().substring(7,19);
            String imageName = null;
            if (null!=image.getRepoTags()){
                imageName = image.getRepoTags()[0];
            }
            if (null == imageName) {
                log.info("出现镜像标签为none，认为无效镜像，跳过");
                return;
            }
            Long size = image.getSize()/1000000;
            DockerImage dockerImage = new DockerImage();
            dockerImage.setCreateDate(TimeUtil.timeToString(created,"yyyy-MM-dd HH:mm:ss"));
            dockerImage.setImgID(imgId);
            dockerImage.setImgName(imageName);
            dockerImage.setRepository(imageName.split(":")[0]);
            dockerImage.setImgVersion(imageName.split(":")[1]);
            dockerImage.setUpdateDate(dtft.format(LocalDateTime.now()));
            dockerImage.setSize(size);
            dockerImage.setStatus(1);
            if (imageName.contains(library)){
                dockerImage.setIfPublish(1);
            }else {
                dockerImage.setIfPublish(0);
            }
            DockerImage findbyImgName = imageDao.findbyImgName(imageName);
            if (null == findbyImgName){
                dockerImages.add(dockerImage);
            }
        });
        imageDao.saveAll(dockerImages);
        return ResultVo.renderOk(dockerImages.size()).withRemark("同步镜像成功");
    }

    @Override
    public void syncOneImage(String repository, String imgVersion) {
        String imgaeName= repository+":"+imgVersion;
        List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(imgaeName).exec();
//        dockerClient.inspectImageCmd("").exec().getContainerConfig().getExposedPorts()
    }

    @Override
    public long count(Map<String, Object> params) {

        Specification specification = new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaQuery.where(list.toArray(new Predicate[list.size()])).getRestriction();
            }
        };
        return  imageDao.count(specification);
    }

    @Override
    public List<DockerImage> findAll(Map<String, Object> params) {
        Specification<DockerImage> specification = new Specification<DockerImage>() {
            @Override
            public Predicate toPredicate(Root<DockerImage> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaQuery.where(list.toArray(new Predicate[list.size()])).getRestriction();
            }
        };
        List<DockerImage> dockerImages = imageDao.findAll(specification);
        return dockerImages;
    }

    @Override
    public List<DockerImage> findAll(Map<String, Object> params, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Specification<DockerImage> specification = new Specification<DockerImage>() {
            @Override
            public Predicate toPredicate(Root<DockerImage> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
//                return criteriaBuilder.and(list.toArray(new Predicate[0]));
                return criteriaQuery.where(list.toArray(new Predicate[list.size()])).getRestriction();
            }
        };
        Page<DockerImage> pageObject = imageDao.findAll(specification, pageable);
        List<DockerImage> dockerImages = new ArrayList<>();
        pageObject.getContent().forEach(tmp -> { dockerImages.add(tmp); });
        return dockerImages;
    }

    @Override
    public DockerImage findOne(String imgID) {
       return imageDao.findByImgID(imgID);
    }

    @Override
    public void loadOneImage(File tarFile) {
        log.info("方法未实现");
    }

    @Override
    public void loadOneImage(InputStream tarInputStream) {
        dockerClient.loadImageCmd(tarInputStream).exec();
    }

    @Autowired
    DockerfileDao dockerfileDao;

    @Override
    public ResultVo buildOneImage(String folder,  String repository, String imgVersion) throws IOException {
        String term = repository + ":" + imgVersion;
        DockerImage findbyImgName = imageDao.findbyImgName(term);
        if (null != findbyImgName) return ResultVo.renderErr().withRemark("镜像已经存在");
        List<Image> imageList = dockerClient.listImagesCmd().withImageNameFilter(term).exec();
        if (imageList.size()>0) return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标镜像已经存在" + term);
        File file = new File(folder);
        boolean exists = file.exists();
        if (!exists) return ResultVo.renderErr().withRemark("临时文件不存在");
        String imageId = dockerClient.buildImageCmd().withDockerfile(file)
                .withNoCache(true).withForcerm(true).start().awaitImageId();
        dockerClient.tagImageCmd(imageId, repository, imgVersion).exec();
        /**处理dockerfile**/
        String dockerfileContent = MyUtil.JdkBase64(IOUtils.toString(new FileInputStream(file)));
        String fileName = repository;
        Dockerfile dockerfile = new Dockerfile();
        dockerfile.setFileName(fileName);
        dockerfile.setFileContent(dockerfileContent);
        Dockerfile byFileName = dockerfileDao.findByFileName(fileName);
        if (null != byFileName){
            dockerfile.setId(byFileName.getId());
            dockerfile.setCreator(byFileName.getCreator());
            dockerfile.setCreateDate(byFileName.getCreateDate());
        }
        dockerfileDao.save(dockerfile);

        Repository oneByEnName = repositoryDao.findOneByEnName(repository);
        long repositoryId = 0l;
        if (null != oneByEnName){
            oneByEnName.getImgNum();
            oneByEnName.setImgNum(oneByEnName.getImgNum()+1);
            repositoryId = oneByEnName.getId();
            repositoryDao.save(oneByEnName);
        }
        DockerImage image = new DockerImage();
        image.setDockerfileContent(dockerfileContent);
        image.setIfPublish(0);
        image.setImgName(term);
        image.setRepositoryId(repositoryId);
        image.setImgID(imageId);
        image.setImgVersion(imgVersion);
        image.setRepository(repository);
        image.setStatus(1);
        DockerImage save = imageDao.save(image);
        return ResultVo.renderOk(save).withRemark("镜像制作成功！");
    }

    @Override
    public ResultVo buildZipImage(String srcFolder,String srcFile, String repository, String imgVersion) throws IOException {
        String term = repository + ":" + imgVersion;
        DockerImage findbyImgName = imageDao.findbyImgName(term);
        if (null != findbyImgName) {
            return ResultVo.renderErr().withRemark("数据库该镜像已经存在");
        }
        List<Image> imageList = dockerClient.listImagesCmd().withImageNameFilter(term).exec();
        if (imageList.size()>0) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("服务器目标镜像已经存在" + term);
        }
        File file = new File(srcFile);
        boolean exists = file.exists();
        if (!exists) {
            return ResultVo.renderErr().withRemark("临时文件不存在");
        }
        String dest = Files.createTempDirectory(Paths.get(srcFolder), "tmp").toFile().getAbsolutePath();
        EnumMap<UnzipEnum, String> unzip = MyUtil.unzip(srcFile, dest);
        if (null != unzip.get(UnzipEnum.ERROR)) {
            return ResultVo.renderErr().withRemark(unzip.get(UnzipEnum.ERROR));
        }
        String targetFolder = unzip.get(UnzipEnum.UNZIP_FOLDER);
        String imageId = dockerClient.buildImageCmd(new File(targetFolder))
                .withNoCache(true).withForcerm(true).start().awaitImageId();
        dockerClient.tagImageCmd(imageId, repository, imgVersion).exec();

        String df = unzip.get(UnzipEnum.DOCKERFILE);
        /**处理dockerfile**/
        String dockerfileContent = MyUtil.JdkBase64(IOUtils.toString(new FileInputStream(df)));
        String fileName = repository;
        Dockerfile dockerfile = new Dockerfile();
        dockerfile.setFileName(fileName);
        dockerfile.setFileContent(dockerfileContent);
        Dockerfile byFileName = dockerfileDao.findByFileName(fileName);
        if (null != byFileName){
            dockerfile.setId(byFileName.getId());
            dockerfile.setCreator(byFileName.getCreator());
            dockerfile.setCreateDate(byFileName.getCreateDate());
        }
        dockerfileDao.save(dockerfile);
        Repository oneByEnName = repositoryDao.findOneByEnName(repository);
        long repositoryId = 0l;
        if (null != oneByEnName){
            oneByEnName.getImgNum();
            oneByEnName.setImgNum(oneByEnName.getImgNum()+1);
            repositoryId = oneByEnName.getId();
            repositoryDao.save(oneByEnName);
        }
        DockerImage dockerImage = new DockerImage();
        dockerImage.setImgName(term);
        dockerImage.setRepository(repository);
        dockerImage.setImgVersion(imgVersion);
        dockerImage.setIfPublish(0);
        dockerImage.setRepositoryId(repositoryId);
        dockerImage.setImgID(imageId);
        imageDao.save(dockerImage);
        return ResultVo.renderOk(imageId).withRemark("镜像制作成功！");
    }

    @Override
    public ResultVo buildOneImage(InputStream inputStream, String repository, String imgVersion) throws InterruptedException {
        String term = repository + ":" + imgVersion;
        DockerImage findbyImgName = imageDao.findbyImgName(term);
        if (null != findbyImgName){
            return ResultVo.renderErr().withRemark("镜像已经存在");
        }
        List<Image> imageList = dockerClient.listImagesCmd().withImageNameFilter(term).exec();
        if (imageList.size() > 0) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标镜像已经存在" + term);
        }
        String imageId = dockerClient.buildImageCmd(inputStream)
                .withNoCache(true)
                .withForcerm(true)
                .start()
                .awaitImageId();
        synchronized (this){
            wait(2000);
        }
        dockerClient.tagImageCmd(imageId, repository, imgVersion).exec();
        Repository oneByEnName = repositoryDao.findOneByEnName(repository);
        long repositoryId = 0l;
        if (null != oneByEnName){
            oneByEnName.getImgNum();
            oneByEnName.setImgNum(oneByEnName.getImgNum()+1);
            repositoryId = oneByEnName.getId();
            repositoryDao.save(oneByEnName);
        }
        DockerImage dockerImage = new DockerImage();
        dockerImage.setImgName(term);
        dockerImage.setRepository(repository);
        dockerImage.setImgVersion(imgVersion);
        dockerImage.setIfPublish(0);
        dockerImage.setImgID(imageId);
        dockerImage.setRepositoryId(repositoryId);
        DockerImage save = imageDao.save(dockerImage);
        return ResultVo.renderOk(save).withRemark("镜像制作成功！");
    }

    @Override
    public ResultVo buildOneImage(File tempFile, String repository, String imgVersion) {
        String term = repository + ":" + imgVersion;
        DockerImage findbyImgName = imageDao.findbyImgName(term);
        if (null != findbyImgName) {
            return ResultVo.renderErr().withRemark("镜像已经存在");
        }
        if (!tempFile.exists()) {
            return ResultVo.renderErr().withRemark("临时文件不存在");
        }
        String imageId = dockerClient.buildImageCmd()
                .withDockerfile(tempFile)
                .withForcerm(true)
                .withNoCache(true)
                .start()
                .awaitImageId();
        dockerClient.tagImageCmd(imageId, repository, imgVersion).exec();
        Repository oneByEnName = repositoryDao.findOneByEnName(repository);
        long repositoryId = 0l;
        if (null != oneByEnName){
            oneByEnName.getImgNum();
            oneByEnName.setImgNum(oneByEnName.getImgNum()+1);
            repositoryId = oneByEnName.getId();
            repositoryDao.save(oneByEnName);
        }
        DockerImage dockerImage = new DockerImage();
        dockerImage.setImgName(term);
        dockerImage.setRepository(repository);
        dockerImage.setImgVersion(imgVersion);
        dockerImage.setIfPublish(0);
        dockerImage.setImgID(imageId);
        dockerImage.setRepositoryId(repositoryId);
        imageDao.save(dockerImage);
        return ResultVo.renderOk(imageId).withRemark("镜像制作成功！");
    }

    @Autowired
    RestTemplate restTemplate;
    @Value("${docker-java.registerUrl}")
    String registerUrl;

    @Override
    public ResultVo pullAll() throws IOException {
        List<String> imageNames = new ArrayList<>();
        List<String> projects = new ArrayList<>();
        /**
         * 1. 获取所有projects
         */
        String register = registerUrl.replace("/library", "");
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://"+register + "/api/projects");
        URI uri = builder.build().encode().toUri();

        ResponseEntity<String> exchange = restTemplate.exchange(uri, HttpMethod.GET, null, String.class);
        String body = exchange.getBody();
        JSONArray jsonArray = JSONObject.parseArray(body);
        for (int i=0; i< jsonArray.size(); i++){
            JSONObject o = (JSONObject) jsonArray.get(i);
            String project_id = String.valueOf(o.get("project_id"));
            projects.add(project_id);
        }
        /**
         * 2. 获取所有repositories
         */
        for (int i=0; i< projects.size(); i++){
            List<String> repositories = new ArrayList<>();
            MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
            map.add("project_id", projects.get(i));
            HttpEntity<MultiValueMap<String, String>> requestEntity =
                    new HttpEntity<MultiValueMap<String, String>>(map,null);
            UriComponentsBuilder builder1 = UriComponentsBuilder.fromHttpUrl("http://"+register + "/api/repositories");
            URI uri1 = builder1.queryParams(map).build().encode().toUri();
            ResponseEntity<String> exchange1 = restTemplate.exchange(uri1,
                    HttpMethod.GET, requestEntity, String.class);
            JSONArray jsonArray1 = JSONObject.parseArray(exchange1.getBody());
            for (int j=0; j<jsonArray1.size(); j++){
                JSONObject o = (JSONObject) jsonArray1.get(j);
                String name = (String) o.get("name");
                repositories.add(name);
            }
            /**
             * 3. 获取所有版本
             */
            for (int j=0; j<repositories.size(); j++){
                UriComponentsBuilder builder2 = UriComponentsBuilder.fromHttpUrl("http://"+register + "/api/repositories/"
                        + repositories.get(j) + "/tags");
                URI uri2 = builder2.build().encode().toUri();
                ResponseEntity<String> exchange2 = restTemplate.exchange(uri2,
                        HttpMethod.GET, null, String.class);
                JSONArray jsonArray2 = JSONObject.parseArray(exchange2.getBody());
                for (int k=0; k<jsonArray2.size(); k++){
                    JSONObject o = (JSONObject) jsonArray2.get(k);
                    String tmpImageName = register+"/"+repositories.get(j)+":"+o.get("name");
                    imageNames.add(tmpImageName);
                }
            }
        }
        log.info("所有镜像如下：" + imageNames);
        File file = new File("images.txt");
        if (!file.exists()){
            file.createNewFile();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter("images.txt"));
        for (String s : imageNames){
            writer.write(s + "\r\n");
        }
        writer.close();

        for (int i=0; i< imageNames.size(); i++){
            boolean b = false;
            try {
                b = dockerClient.pullImageCmd(imageNames.get(i))
                        .withAuthConfig(authConfig)
                        .withRegistry(registerUrl)
                        .start().awaitCompletion(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                log.info("镜像 " + imageNames.get(i) + "下载结果 : " + b);
            }
        }

        return ResultVo.renderOk("拉取所有仓库镜像成功！");
    }

    @Override
    public ResultVo buildOneimage(String bucket, String fileName,String repository, String imgVersion) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        boolean b = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!b) return ResultVo.renderErr().withRemark("目标文件不存在");
        InputStream object = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(fileName).build());
        String imageId = dockerClient.buildImageCmd(object)
                .withNoCache(true)
                .withForcerm(true)
                .start()
                .awaitImageId();

        dockerClient.tagImageCmd(imageId, repository, imgVersion).exec();
        return ResultVo.renderOk(imageId).withRemark("镜像制作成功！");

    }

    @Autowired
    AuthConfig authConfig;

    @Override
    public ResultVo pushOneImage(String repository, String imgVersion) throws InterruptedException {
        String imgName = repository+":"+imgVersion;
        DockerImage dockerImage = imageDao.findbyImgName(imgName);
        if (null == dockerImage) {
            return ResultVo.renderErr().withRemark("目标镜像不存在");
        }
        String imageNameWithRepository = authConfig.getRegistryAddress()+"/"+imgName;
        dockerClient.tagImageCmd(dockerImage.getImgID(), imageNameWithRepository, imgVersion).exec();
        boolean b = dockerClient.pushImageCmd(imgName)
                .withAuthConfig(authConfig)
                .withName(imageNameWithRepository)
                .withTag(imgVersion).start().awaitCompletion(60, TimeUnit.SECONDS);
        if (!b){
            return ResultVo.renderErr().withRemark("推送到仓库失败，可能由于网速过慢");
        }
        dockerImage.setIfPublish(1);
        dockerImage.setImgName(imgName);
        imageDao.save(dockerImage);
        return ResultVo.renderOk(imageNameWithRepository).withRemark("推送到仓库成功");
    }

    @Override
    public ResultVo pullOneImage(String registry, String repository, String imgVersion) throws InterruptedException {
        boolean b = dockerClient.pullImageCmd(repository+":"+imgVersion)
                .withAuthConfig(authConfig)
                .withRegistry(registry)
                .start().awaitCompletion(60, TimeUnit.SECONDS);

        if (!b) {
            return ResultVo.renderErr().withRemark("拉取失败，可能由于镜像不存在");
        }
        return ResultVo.renderOk(repository).withRemark("拉取镜像成功！");
    }
    @Override
    public ResultVo pullOneImage(String registry, String repository, String imgVersion,
                                 String userName, String passWord, String description) throws InterruptedException {

        AuthConfig authConfig1 = authConfig;
        if (Strings.isNotEmpty(registry) && Strings.isNotEmpty(userName) && Strings.isNotEmpty(passWord)){
            //临时授权
            authConfig1 =  new AuthConfig()
                    .withRegistryAddress(registry)
                    .withUsername(userName)
                    .withPassword(passWord);
            AuthResponse exec = dockerClient.authCmd().withAuthConfig(authConfig1).exec();
            authConfig1.withIdentityToken(exec.getIdentityToken());

        }

        boolean b = dockerClient.pullImageCmd(repository+":"+imgVersion)
                .withAuthConfig(authConfig1)
                .withRegistry(registry)
                .start().awaitCompletion(60, TimeUnit.SECONDS);

        if (!b) {
            return ResultVo.renderErr().withRemark("拉取失败，可能由于镜像不存在");
        }
        return ResultVo.renderOk(repository).withRemark("拉取成功！");
    }

    @Override
    public ResultVo deleteOneImage(String imgName) {
        DockerImage dockerImage = imageDao.findbyImgName(imgName);
        if (null == dockerImage) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标镜像不存在");
        }
        try {
            dockerClient.removeImageCmd(dockerImage.getImgID()).withForce(true).exec();
            imageDao.delete(dockerImage);
            Repository oneByEnName = repositoryDao.findOneByEnName(imgName);
            if (null != oneByEnName){
                repositoryDao.delete(oneByEnName);
            }
        }catch (Exception e){
            imageDao.delete(dockerImage);
            log.info("原因：, { }", e.getMessage());
            return ResultVo.renderErr().withRemark("删除镜像出错，可能由于该镜像实际已经删除");
        }
        return ResultVo.renderOk().withRemark("删除镜像成功" + imgName);
    }

    @Override
    public ResultVo getImgDetail(String imgName) {
        DockerImage dockerImage = imageDao.findbyImgName(imgName);
        if (null == dockerImage) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标镜像不存在");
        }
        InspectImageResponse exec = dockerClient.inspectImageCmd(dockerImage.getImgID()).exec();
        DockerImageVo vo = new DockerImageVo();
        vo.setAuthor(exec.getAuthor());
        vo.setBuild(exec.getArch()+exec.getDockerVersion()+exec.getOs());
        vo.setCreated(exec.getCreated());
        vo.setImgID(dockerImage.getImgID());
        vo.setParentId(exec.getParent());
        vo.setSize(exec.getSize());
        vo.setDockerFile(null == dockerImage.getDockerfileContent() ? "暂时不支持解析非平台制作的镜像" : dockerImage.getDockerfileContent());
        return ResultVo.renderOk(vo).withRemark("查询镜像详情成功！" + imgName);
    }

    @Override
    public InputStream exportImage(String imgName) {
        DockerImage dockerImage = imageDao.findbyImgName(imgName);
        if (null == dockerImage){
            return null;
        }
        InputStream exec = dockerClient.saveImageCmd(imgName).exec();
        return exec;
    }

    @Override
    public ResultVo tagOneImage(String repository, String imgVersion) {
        String imageName = repository+":"+imgVersion;
        DockerImage dockerImage = imageDao.findbyImgName(imageName);
        if (null == dockerImage) {
            return ResultVo.renderErr().withRemark("打标签失败，数据库中不存在该镜像，可能由于未同步数据导致");
        }
        dockerClient.tagImageCmd(dockerImage.getImgID(), repository, imgVersion).exec();
        return ResultVo.renderOk(dockerImage.getImgID()).withRemark("打标签成功,您可以使用新的标签访问该镜像");
    }
}
