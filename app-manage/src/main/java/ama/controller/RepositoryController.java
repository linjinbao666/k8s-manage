package ama.controller;

import ama.entity.Repository;
import ama.service.RepositoryService;
import ama.vo.ResultVo;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.directory.api.util.Strings;
import org.simpleframework.xml.core.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 镜像仓库
 */

@Slf4j
@Api(value = "镜像仓库")
@RestController
@RequestMapping(value = "/repository")
public class RepositoryController {

    @Autowired
    RepositoryService repositoryService;

    @ApiOperation("查询仓库列表-带分页")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "cnName", value = "中文名称"),
            @ApiImplicitParam(name = "enName", value = "英文名称"),
            @ApiImplicitParam(name = "pageNum", value = "页码"),
            @ApiImplicitParam(name = "type", value = "类型"),
            @ApiImplicitParam(name = "pageSize", value = "分页大小")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询镜像列表成功")
    })
    @GetMapping(value = "")
    public ResultVo repository(
            @RequestParam(value = "cnName", required = false) String cnName,
            @RequestParam(value = "enName", required = false) String enName,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize
    ){
        Map<String, Object> params = new HashMap<String, Object>();
        if (Strings.isNotEmpty(cnName)){
            params.put("cnName", cnName);
        }

        if (Strings.isNotEmpty(enName)){
            params.put("enName", enName);
        }

        if(Strings.isNotEmpty(type)){
            params.put("type", type);
        }

        List<Repository> repositories = repositoryService.find(params, pageSize, pageNum-1);
        long count = repositoryService.count(params);
        Map<String, Object> result = new HashMap<String, Object>(16);
        result.put("items", repositories);
        result.put("count", count);
        return ResultVo.renderOk(result).withRemark("查询仓库列表成功！");
    }

    @ApiOperation("创建仓库")
    @ApiResponse(code = 200, message = "新增成功！")
    @PostMapping(value = "/addOne")
    public ResultVo<Repository> addOne(@Validated Repository repository){
        return  repositoryService.addOne(repository);
    }

    @ApiOperation("更新仓库")
    @PostMapping(value = "/updataOne")
    public ResultVo updateOne(
            @RequestParam(value = "id", required = true) Long id,
            @RequestParam(value = "type", required = true) String type,
            @RequestParam(value = "description", required = true) String description
    ){
        Repository repository = new Repository();
        repository.setId(id);
        repository.setType(type);
        repository.setDescription(description);
        return repositoryService.updateOne(repository);
    }

    @ApiOperation("删除仓库")
    @ApiImplicitParam(name = "id", value = "记录id")
    @PostMapping(value = "/deleteOne")
    public ResultVo deleteOne(Long id){
        return repositoryService.deleteOne(id);
    }

}
