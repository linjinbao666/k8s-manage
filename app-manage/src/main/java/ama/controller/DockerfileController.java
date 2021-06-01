package ama.controller;

import ama.entity.Dockerfile;
import ama.service.DockerfileService;
import ama.enumlation.CodeEnum;
import ama.util.MyUtil;
import ama.vo.ResultVo;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.directory.api.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dockerfile 接口
 */

@Slf4j
@Api("镜像配置接口")
@RestController
@RequestMapping("/dockerfile")
public class DockerfileController {
    @Autowired
    DockerfileService dockerfileService;
    @ApiOperation("列出所有dockerfile文件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "fileName",value = "dockerfile文件名称", required = false, example = "vcode"),
            @ApiImplicitParam(name = "pageNum",value = "页面",required = false,example = "1"),
            @ApiImplicitParam(name = "pageSize",value = "分页大小",required = false, example = "5"),
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功")
    })
    @GetMapping("/")
    public ResultVo dockerfile(
            @RequestParam(value = "fileName", required = false)String fileName,
            @RequestParam(value = "pageNum",required = false, defaultValue = "1")Integer pageNum,
            @RequestParam(value = "pageSize",required = false, defaultValue = "10")Integer pageSize
    ){
        Map params = new HashMap();
        if (Strings.isNotEmpty(fileName)){
            params.put("fileName", fileName);
        }
        List items = dockerfileService.findAll(params, pageNum-1, pageSize);
        long count = dockerfileService.count(params);
        Map result = new HashMap();
        result.put("count", count);
        result.put("items", items);
        return ResultVo.renderOk(result).withRemark("查询成功！");
    }

    @GetMapping("/dockerfileAll")
    public ResultVo dockerfileAll(
            @RequestParam(value = "fileName", required = false)String fileName
    ){
        Map<String, Object> params = new HashMap();
        if (Strings.isNotEmpty(fileName)){
            params.put("fileName", fileName);
        }
        List items = dockerfileService.findAll(params);
        long count = dockerfileService.count(params);
        Map result = new HashMap();
        result.put("count", count);
        result.put("items", items);
        return ResultVo.renderOk(result).withRemark("查询成功！");
    }

    /**
     * 新增dockerfile
     * @return
     */
    @ApiOperation("新增Dockerfile")
    @ApiResponse(code = 200, message = "新增成功")
    @PostMapping("/")
    public ResultVo addDockerfile(Dockerfile dockerfile){
        if (Strings.isEmpty(dockerfile.getFileName())) return ResultVo.renderErr(CodeEnum.ERR).withRemark("文件名不能为空");
        if (!MyUtil.isBase64(dockerfile.getFileContent())) return ResultVo.renderErr().withRemark("新增失败，请将文件格式使用base64加密");
        return dockerfileService.addOne(dockerfile);
    }

    @ApiOperation("修改指定Dockerfile")
    @PutMapping(value = "/{fileName}")
    public ResultVo updateDockerfile(Dockerfile dockerfile){
        if (null == dockerfile.getFileName()) return ResultVo.renderErr().withRemark("参数为空");
        if (!MyUtil.isBase64(dockerfile.getFileContent())) return ResultVo.renderErr().withRemark("修改失败，请将文件格式使用base64加密");
        return dockerfileService.updateOne(dockerfile);
    }

    /**
     * 删除dockerfile
     * @param fileName
     * @return
     */

    @DeleteMapping("/{fileName}")
    public ResultVo deleteDockerfile(@PathVariable("fileName")String fileName){
        if (Strings.isEmpty(fileName)) return ResultVo.renderErr(CodeEnum.ERR).withRemark("文件名不能为空");
        return dockerfileService.deleteOne(fileName);
    }
}
