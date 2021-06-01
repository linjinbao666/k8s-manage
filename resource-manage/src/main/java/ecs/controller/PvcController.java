package ecs.controller;

import com.jcraft.jsch.SftpException;
import ecs.entity.Pvc;
import ecs.exception.BizException;
import ecs.service.ApprovalService;
import ecs.service.PvcService;
import ecs.upload.FileProgressMonitor;
import ecs.vo.Approval;
import ecs.vo.CodeEnum;
import ecs.vo.ResultVo;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "存储资源管理接口")
@RestController
@Slf4j
@RequestMapping(value = "/pvc")
public class PvcController {

    @Autowired
    private PvcService pvcService;

    @Autowired
    private ApprovalService approvalService;

    @ApiOperation(value = "查询存储卷", httpMethod = "GET")
    @GetMapping(value = "/findAllPvc")
    public ResultVo find(
            @RequestParam(value = "pvcName", required = false) String pvcName,
            @RequestParam(value = "namespace", required = false)String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1")Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "5")Integer pageSize
    ) {
        pvcService.syncPvc();
        Map<String, Object> params = new HashMap<>();
        if(Strings.isNotEmpty(pvcName)) params.put("pvcName", pvcName);
        if (Strings.isNotEmpty(namespace)) params.put("namespace", namespace);
        List<Pvc> list = pvcService.find(params, pageNum-1, pageSize);
        long count = pvcService.findCount();
        Map map = new HashMap(){{
            put("item", list);
            put("count", count);
        }};
        return ResultVo.renderOk(map);
    }

    @GetMapping(value = "/getDetail")
    public ResultVo getDetail(
            @RequestParam(value = "pvcName", required = false) String pvcName,
            @RequestParam(value = "namespace", required = false)String namespace
    ){
        return pvcService.findPvcDetail(namespace,pvcName);
    }

    @ApiOperation(value = "创建存储卷", httpMethod = "POST")
    @PostMapping(value = "/createPvc")
    public ResultVo createPvc(@Validated Pvc pvc){
        return pvcService.createPvc(pvc);
    }

    @ApiOperation(value = "手动同步存储卷信息", httpMethod = "GET")
    @ApiResponse(code = 200, message = "同步成功！")
    @GetMapping(value = "/syncPvc")
    public ResultVo SyncPvc(){
        pvcService.syncPvc();
        return ResultVo.renderOk();
    }

    @ApiOperation(value = "删除存储卷", httpMethod = "POST")
    @PostMapping(value = "/deletePvc")
    public ResultVo deletePvc(
            @RequestParam(value = "pvcName")String pvcName,
            @RequestParam(value = "namespace")String namespace
    ){
        return pvcService.deletePvc(namespace,pvcName);
    }

    @ApiOperation(value = "列出存储卷中文件", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "pvcName", name = "存储卷名称", required = true, example = "text-pvc"),
            @ApiImplicitParam(value = "namespace", name = "名称空间", required = true, example = "fline"),
            @ApiImplicitParam(value = "relativePath", required = false, defaultValue = "/")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功！"),
            @ApiResponse(code = 201, message = "查询失败！")
    })
    @GetMapping(value = "/listFiles")
    public ResultVo listFiles(
            @RequestParam(value = "keyword", required = false)String keyword,
            @RequestParam(value = "sort", required = false, defaultValue = "date") String sort,
            @RequestParam(value = "order", required = false, defaultValue = "asc") String order,
            @RequestParam(value = "pvcName")String pvcName,
            @RequestParam(value = "namespace",required = false, defaultValue = "fline")String namespace,
            @RequestParam(value = "relativePath", required = false, defaultValue = "/")String relativePath){
        Pvc pvc = new Pvc();
        pvc.setPvcName(pvcName);
        pvc.setNamespace(namespace);
        if(!relativePath.startsWith("/") || !relativePath.endsWith("/")){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("需要以/开头和结束");
        }
        return pvcService.findFilesInVolumes(namespace, pvcName, relativePath, sort, order, keyword);
    }

    @ApiOperation("下载文件")
    @RequestMapping("/fileDownload")
    public void fileDownload(
            HttpServletResponse response,
            @RequestParam(value = "pvcName")String pvcName,
            @RequestParam(value = "namespace", required = false, defaultValue = "fline")String namespace,
            @RequestParam(value = "filePath")String filePath,
            @RequestParam(value = "fileName")String fileName) throws SftpException, IOException {

        if (!filePath.startsWith("/")) throw new BizException(CodeEnum.ERR).withRemark("路径参数错误，需要以/开头");

        if (fileName.contains("/")){
            throw new BizException(CodeEnum.ERR).withRemark("文件名称非法");
        }

        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename="+ URLEncoder.encode(fileName, "UTF-8"));

        InputStream download = pvcService.download(namespace, pvcName, filePath, fileName);

        if(null == download){
            log.error("下载失败");
        }

        try(BufferedInputStream bis = new BufferedInputStream(download);){
            byte[] buff = new byte[1024];
            OutputStream os  = response.getOutputStream();
            int i = 0;
            while ((i = bis.read(buff)) != -1) {
                os.write(buff, 0, i);
                os.flush();
            }
        }catch (Exception e){
            log.error("文件下载出错"+ e.getMessage());
        }finally {
            download.close();
        }

    }

    @ApiOperation("新建文件夹")
    @PostMapping(value = "/newFolder")
    public ResultVo newFolder(
            @RequestParam(value = "pvcName")String pvcName,
            @RequestParam(value = "namespace", required = false, defaultValue = "fline")String namespace,
            @RequestParam(value = "relativePath", required = false, defaultValue = "/")String relativePath,
            @RequestParam(value = "folderName") String folderName){

        if(!relativePath.startsWith("/") || !relativePath.endsWith("/")) return ResultVo.renderErr(CodeEnum.ERR).withRemark("需要以/开头和结束");
        if (folderName.contains("/")) return ResultVo.renderErr(CodeEnum.ERR).withRemark("新建的文件夹名称，不要带有/符号");

        return pvcService.newFolder(namespace, pvcName, relativePath, folderName);
    }

    @ApiOperation(value = "删除存储卷中的文件", httpMethod = "POST")
    @RequestMapping(value = "/deleteFiles")
    public ResultVo deleteFiles(
            @RequestParam(value = "pvcName")String pvcName,
            @RequestParam(value = "namespace", required = false, defaultValue = "fline")String namespace,
            @RequestParam(value = "filePaths")List<String> filePaths
    ){
        if (filePaths.isEmpty()) return ResultVo.renderErr(CodeEnum.ERR).withRemark("请检查参数");
        return pvcService.deleteFiles(namespace, pvcName, filePaths);
    }

    @ApiOperation(value = "上传文件到存储卷", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "pvcName", name = "存储卷名称", required = true, example = "test-pvc1"),
            @ApiImplicitParam(value = "namespace", name = "名称空间", required = true, example = "fline"),
            @ApiImplicitParam(value = "path", name = "相对路径", required = true, example = "/a/b"),
            @ApiImplicitParam(value = "path", name = "文件，使用二进制文件流", required = true, example = "jdk.exe")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "上传成功！"),
            @ApiResponse(code = 201, message = "上传失败")
    })
    @PostMapping(value = "/upload")
    public ResultVo uploadFile(
            @RequestParam(value = "pvcName")String pvcName,
            @RequestParam(value = "namespace")String namespace,
            @RequestParam(value = "path")String path,
            @RequestParam(value = "file") MultipartFile file){

        return pvcService.uploadFile(namespace, pvcName, path, file);
    }

    @ApiOperation(value = "查找存储信息", httpMethod = "GET")
    @ApiResponse(code = 200, message = "查找成功")
    @GetMapping(value = "/findStoragerInfo")
    ResultVo findStoragerInfo(){
        return pvcService.getStoragerInfo();
    }

    @ApiOperation(value = "获取文件上传进度", httpMethod = "GET")
    @ApiImplicitParam(value = "uploadFileName", name = "文件名称", required = true, example = "jdk.exe", dataType = "String")
    @ApiResponse(code = 200, message = "")
    @GetMapping(value = "/getUploadPercent")
    public ResultVo getUploadPercent(@RequestParam(value = "uploadFileName")String uploadFileName){
        String proGress = FileProgressMonitor.getMap().get(uploadFileName);
        if(proGress==null) {
           return ResultVo.renderOk().withRemark("传输未开始，或者已传输结束");
        }else{
           return ResultVo.renderOk(proGress);
        }
    }

}
