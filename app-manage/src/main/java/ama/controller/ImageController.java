package ama.controller;

import ama.entity.DockerImage;
import ama.service.ImageService;
import ama.enumlation.CodeEnum;
import ama.vo.ResultVo;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.directory.api.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Api(value = "镜像管理")
@RestController
@RequestMapping(value = "/image")
public class ImageController {

    final ReentrantLock lock = new ReentrantLock();

    @Autowired
    ImageService imageService;

    @ApiOperation(value = "同步服务器镜像到数据库")
    @ApiResponse(code = 200, message = "同步镜像成功")
    @GetMapping(value = "/syncImage")
    public ResultVo syncImage() throws InterruptedException {
        ResultVo resultVo = new ResultVo();
        if (lock.tryLock()){
           try {
               resultVo = imageService.syncImages();
           }finally {
               lock.unlock();
           }
           return resultVo;
        }
        resultVo = new ResultVo(CodeEnum.ERR).withRemark("同步任务执行中，请勿重复操作");
        return resultVo;
    }

    @ApiOperation("查询全部镜像-不带分页")
    @ApiResponse(code = 200, message = "查询成功")
    @GetMapping("/image")
    public ResultVo image(
            @RequestParam(value = "repositoryId", required = false) Long repositoryId,
            @RequestParam(value = "repository", required = false) String repository,
            @RequestParam(value = "ifPublish", required = false) Integer ifPublish
    ){
        Map<String, Object> params = new HashMap<>();
        if (null != repositoryId) {
            params.put("repositoryId", repositoryId);
        }
        if (null != repository) {
            params.put("repository", repository);
        }
        if (null != ifPublish) {
            params.put("ifPublish", ifPublish);
        }
        List<DockerImage> items = imageService.findAll(params);
        return ResultVo.renderOk(items).withRemark("查询全部镜像成功");
    }

    @ApiOperation("查询镜像列表-带分页")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "repositoryId", value = "镜像仓库ID"),
            @ApiImplicitParam(name = "repository", value = "镜像仓库名称"),
            @ApiImplicitParam(name = "pageNum", value = "页码"),
            @ApiImplicitParam(name = "pageSize", value = "分页大小")
    })
    @ApiResponses({@ApiResponse(code = 200, message = "查询镜像列表成功")})
    @GetMapping(value = "/")
    public ResultVo image(
            @RequestParam(value = "repositoryId", required = false) Long repositoryId,
            @RequestParam(value = "repository", required = false) String repository,
            @RequestParam(value = "ifPublish", required = false) Integer ifPublish,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize
    ){
        Map params = new HashMap<>();
        if (null != repositoryId) {
            params.put("repositoryId", repositoryId);
        }
        if (null != repository) {
            params.put("repository", repository);
        }
        if (null != ifPublish) {
            params.put("ifPublish", ifPublish);
        }
        List items = imageService.findAll(params, pageNum-1, pageSize);
        long count = imageService.count(params);
        Map result = new HashMap();
        result.put("items", items);
        result.put("count", count);

        return ResultVo.renderOk(result).withRemark("查询镜像列表成功");
    }

    @ApiOperation("根据imgName获取镜像详情")
    @ApiImplicitParam(name = "imgName", value = "镜像名称", required = true)
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询镜像详情成功"),
            @ApiResponse(code = 201, message = "镜像名称非法"),
            @ApiResponse(code = 201, message = "目标镜像不存在"),
    })
    @GetMapping(value = "/imageDetail")
    public ResultVo imageDetail(@RequestParam(value = "imgName")String imgName){
        if (null == imgName) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("镜像名称非法");
        }
        return imageService.getImgDetail(imgName);
    }

    /**
     * 老接口，会因为镜像名称中带有/而失效
     * @param imgName
     * @return
     */
    @Deprecated
    @GetMapping(value = "/{imgName}")
    public ResultVo imageDetail1(@PathVariable(value = "imgName")String imgName){
        if (null == imgName) return ResultVo.renderErr(CodeEnum.ERR).withRemark("镜像名称非法");
        return imageService.getImgDetail(imgName);
    }

    @ApiOperation("imgName，删除指定镜像")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "imgName", value = "镜像名称", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "删除镜像成功"),
            @ApiResponse(code = 201, message = "镜像名称非法"),
            @ApiResponse(code = 201, message = "删除镜像失败"),
    })
    @ResponseBody
    @DeleteMapping(value = "/imageDelete")
    public ResultVo imageDelete(
            @RequestParam(value = "imgName")String imgName
    ){
        if (null == imgName) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("镜像名称非法");
        }
        return imageService.deleteOneImage(imgName);
    }

    @ApiOperation("上传压缩包制作镜像，PS:此压缩包不是导出的镜像包")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "repository", value = "仓库名称", required = true),
            @ApiImplicitParam(name = "imgVersion", value = "镜像版本", required = true),
            @ApiImplicitParam(name = "file", value = "文件二进制流", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "镜像制作成功"),
            @ApiResponse(code = 201, message = "文件内容为空，上传失败"),
            @ApiResponse(code = 201, message = "上传文件出错"),
            @ApiResponse(code = 201, message = "目标镜像已经存在"),
    })
    @PostMapping("/buildImage")
    public ResultVo buildImage(
            @RequestParam("repository") String repository,
            @RequestParam("imgVersion") String imgVersion,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (null == file) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("文件内容为空，上传失败");
        }
        if (Strings.isEmpty(repository)) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("仓库名称为空");
        }
        if (Strings.isEmpty(imgVersion)) imgVersion = "latest";
        String originalFilename = file.getOriginalFilename();
        String suffixName = originalFilename.substring(originalFilename.lastIndexOf("."));
        InputStream inputStream = null;
        ResultVo resultVo = new ResultVo();
        try {
            inputStream = file.getInputStream();
            resultVo = imageService.buildOneImage(inputStream,repository,imgVersion);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("上传文件出错" + e.getMessage());
        }finally {
            inputStream.close();
        }
        return resultVo;
    }

    @ApiOperation("上传ZIP压缩包制作镜像")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "repository", value = "仓库名称", required = true),
            @ApiImplicitParam(name = "imgVersion", value = "镜像版本", required = true),
            @ApiImplicitParam(name = "file", value = "Dockerfile文件二进制流", required = true)
    })
    @PostMapping("/buildImageZip")
    public ResultVo buildImageZip(
            @RequestParam("repository") String repository,
            @RequestParam("imgVersion") String imgVersion,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (null == file) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("文件内容为空，上传失败");
        }
        if (Strings.isEmpty(repository)) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("仓库名称为空");
        }
        if (Strings.isEmpty(imgVersion)) {
            imgVersion = "latest";
        }
        File tmpFolder = Files.createTempDirectory(repository).toFile();
        File tmpFile = File.createTempFile(file.getOriginalFilename(),".tmp", tmpFolder);
        file.transferTo(tmpFile);
        ResultVo resultVo = imageService.buildZipImage(tmpFolder.getAbsolutePath(),
                tmpFile.getAbsolutePath(), repository, imgVersion);
        tmpFile.delete();
        tmpFolder.delete();
        return resultVo;
    }

    @ApiOperation("在线制作")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "repository", value = "镜像仓库", required = true),
            @ApiImplicitParam(name = "imgVersion", value = "镜像版本", required = true),
            @ApiImplicitParam(name = "file", value = "Dockerfile文件二进制流", required = true)
    })
    @PostMapping("/buildImageFromDockerfile")
    public ResultVo buildImageFromDockerfile(
            @RequestParam("repository") String repository,
            @RequestParam("imgVersion") String imgVersion,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        File tmpFolder = Files.createTempDirectory(repository).toFile();
        File tmpFile = File.createTempFile(file.getOriginalFilename(),".tmp", tmpFolder);
        file.transferTo(tmpFile);
        ResultVo resultVo =  imageService.buildOneImage(tmpFile.getAbsolutePath(), repository, imgVersion);
        tmpFile.delete();
        tmpFolder.delete();
        return resultVo;
    }

    @ApiOperation("使用Dockergfile内容构建镜像")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "repository", value = "镜像仓库", required = true),
            @ApiImplicitParam(name = "imgVersion", value = "镜像版本", required = true),
            @ApiImplicitParam(name = "fileContent", value = "Dockerfile文件内容", required = true)
    })
    @PostMapping("/buildImageFromDockerfileContent")
    public ResultVo buildImageFromDockerfileContent(
            @RequestParam("repository") String repository,
            @RequestParam("imgVersion") String imgVersion,
            @RequestParam("fileContent") String fileContent
    ) throws IOException {
        File tempFile = File.createTempFile(repository, ".temp");
        FileOutputStream fos = new FileOutputStream(tempFile);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        osw.write(fileContent);
        osw.flush();
        ResultVo resultVo = imageService.buildOneImage(tempFile,repository, imgVersion);
        tempFile.delete();
        return resultVo;
    }

    @ApiOperation("从远程链接导入制作镜像")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "repository", value = "镜像repository", required = true),
            @ApiImplicitParam(name = "imgVersion", value = "镜像版本", required = true),
            @ApiImplicitParam(name = "url", value = "远程链接", required = true)
    })
    @ApiResponse(code = 201, message = "接口未实现")
    @PostMapping(value = "/buildImageFromUrl")
    public ResultVo buildImageFromUrl(
            @RequestParam("repository") String repository,
            @RequestParam("imgVersion") String imgVersion,
            @RequestParam("url")String url
    ){
        return ResultVo.renderErr().withRemark("接口未实现");
    }

    @ApiOperation("导入镜像tar包，非原始制作文件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", value = "文件二进制流")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "镜像包导入成功"),
            @ApiResponse(code = 201, message = "tar包上传出错"),
            @ApiResponse(code = 201, message = "文件内容为空，上传失败")
    })
    @PostMapping(value = "/importImage")
    public ResultVo importImage(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (null == file) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("文件内容为空，上传失败");
        }
        ResultVo resultVo = new ResultVo();
        String originalFilename = file.getOriginalFilename();
        if (lock.tryLock()){
            InputStream inputStream = null;
            try{
                inputStream = file.getInputStream();
                imageService.loadOneImage(inputStream);
            }catch (Exception e){
                return ResultVo.renderErr(CodeEnum.ERR).withRemark("tar包上传出错" + e.getMessage());
            } finally {
                inputStream.close();
                lock.unlock();
            }
            return ResultVo.renderOk().withRemark("镜像包导入成功" + originalFilename);
        }
        resultVo = new ResultVo(CodeEnum.ERR).withRemark("导入任务存在，请稍后再试");
        return resultVo;
    }

    /**
     * 导出镜像tar包
     */
    @ApiOperation("导出镜像")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "imgName", value = "镜像名称", required = true)
    )
    @GetMapping(value = "/exportImage")
    public String exportImage(
            HttpServletResponse response,
            @RequestParam("imgName")String imgName
    ) throws IOException {
        if (Strings.isEmpty(imgName)) {
            return "文件名为空，下载失败";
        }
        if (lock.tryLock()){
            OutputStream outputStream = response.getOutputStream();
            response.setContentType("application/x-download");
            response.addHeader("Content-Disposition", "attachment;filename="
                    +URLEncoder.encode(imgName, "UTF-8")+".tar");
            try {
                InputStream download = imageService.exportImage(imgName);
                IOUtils.copy(download, outputStream);
                outputStream.flush();
            }catch (Exception e){
                e.printStackTrace();
                return "导出失败"+e.getMessage();
            }finally {
                lock.unlock();
            }
            return "导出镜像成功！";
        }
        return "有导出操作进行中，请稍后操作";
    }

    @ApiOperation("上传镜像到仓库接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "repository", value = "镜像仓库", required = true),
            @ApiImplicitParam(name = "imgVersion", value = "镜像版本", required = true)
    })
    @PostMapping("/pushImage")
    public ResultVo pushImage(
            String repository,
            String imgVersion
    )  {
        ResultVo resultVo = new ResultVo();
        if (lock.tryLock()){
            try {
                resultVo = imageService.pushOneImage(repository, imgVersion);
            }catch (Exception e){
                e.printStackTrace();
                resultVo = new ResultVo(CodeEnum.ERR).withRemark("推送出错"+e.getMessage());
            }finally {
                lock.unlock();
            }
        }else {
            resultVo = new ResultVo(CodeEnum.ERR).withRemark("推送操作进行中，请稍后尝试");
        }
        return resultVo;
    }

    @ApiOperation("下拉镜像")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "registry", value = "仓库地址", required = false),
            @ApiImplicitParam(name = "repository", value = "仓库名称", required = true),
            @ApiImplicitParam(name = "imgVersion", value = "镜像版本", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "pull成功"),
            @ApiResponse(code = 201, message = "pull失败，可能由于镜像不存在")
    })
    @GetMapping("/pullImage")
    public ResultVo pullImage(
            @RequestParam(value = "registry", required = false) String registry,
            String repository,
            String imgVersion,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "passWord", required = false) String passWord,
            @RequestParam(value = "description", required = false) String description
    ) throws InterruptedException {
        ResultVo resultVo = new ResultVo();
        if (lock.tryLock()){
            try {
                resultVo = imageService.pullOneImage(registry, repository, imgVersion, userName, passWord, description);
            }catch (Exception e){
                log.info(e.getMessage());
                resultVo = new ResultVo(CodeEnum.ERR).withRemark("错误原因："+ e.getMessage());
            }finally {
                lock.unlock();
            }
        }else {
            resultVo = new ResultVo(CodeEnum.ERR).withRemark("拉取操作进行中，请稍后操作");
        }
       return resultVo;
    }

    @ApiOperation("为镜像打标签")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "repository", value = "镜像repository", required = true),
            @ApiImplicitParam(name = "imgVersion", value = "镜像版本", required = true),
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "打标签成功,您可以使用新的标签访问该镜像"),
            @ApiResponse(code = 201, message = "打标签失败，数据库中不存在该镜像，可能由于未同步数据导致"),
    })
    @PostMapping("/tagImage")
    public ResultVo tagImage(
        String repository,
        String imgVersion
    ){
        return imageService.tagOneImage(repository, imgVersion);
    }

    @GetMapping("/pullAllImage")
    public ResultVo pullAllImage(){
        ResultVo resultVo = new ResultVo();
        if (lock.tryLock()){
            try {
                resultVo = imageService.pullAll();
            }catch (Exception e){
                log.info(e.getMessage());
                resultVo = new ResultVo(CodeEnum.ERR).withRemark("错误原因："+ e.getMessage());
            }finally {
                lock.unlock();
            }
        }else {
            resultVo = new ResultVo(CodeEnum.ERR).withRemark("拉取hub镜像操作进行中，请稍后操作");
        }
        return resultVo;
    }

    @GetMapping("/validateImage")
    public ResultVo validateImage(
            @RequestParam("repository") String repository,
            @RequestParam("imgVersion") String imgVersion
    ){
        Map<String, Object> params = new HashMap<>();
        if (null != repository) {
            params.put("repository", repository);
        }
        if (null != imgVersion) {
            params.put("imgVersion", imgVersion);
        }
        List all = imageService.findAll(params);
        if (all.isEmpty()) {
            return ResultVo.renderOk(true).withRemark("数据库不存在，校验通过");
        }
        return ResultVo.renderOk(false).withRemark("数据库已经存在，校验不通过");
    }


    @ApiOperation("制作镜像通用接口")
    @PostMapping("/buildOne")
    public ResultVo buildOne(
            @RequestParam("type") Integer type,
            @RequestParam("repository") String repository,
            @RequestParam("imgVersion") String imgVersion,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "registry", required = false) String registry,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "passWord", required = false) String passWord,
            @RequestParam(value = "description", required = false) String description
    ) throws IOException, InterruptedException {
        switch (type){
            case 1:
                return importImage(file);
            case 2:
                return buildImageFromDockerfile(repository, imgVersion, file);
            case 3:
                return buildImageZip(repository, imgVersion, file);
            case 4:
                return pullImage(registry, repository, imgVersion, userName, passWord, description);
            default:
                break;
        }
        return ResultVo.renderVain();
    }
}
