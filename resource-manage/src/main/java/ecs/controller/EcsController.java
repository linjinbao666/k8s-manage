package ecs.controller;

import ecs.entity.EcsResource;
import ecs.exception.ServerException;
import ecs.service.EcsResourceService;
import ecs.service.impl.EcsResourceServiceImpl;
import ecs.util.LinuxUtil;
import ecs.vo.CodeEnum;
import ecs.vo.RangeTypeEnum;
import ecs.vo.ResultVo;
import io.swagger.annotations.*;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "服务器管理接口")
@CrossOrigin
@RestController
@RequestMapping(value = "/ecsresource")
public class EcsController {
    @Autowired
    private EcsResourceService ecsResourceService;

    @ApiOperation(value = "服务器列表分页查询", httpMethod = "GET")
    @GetMapping(value = "/findECSPagenation")
    public ResultVo find(
            @RequestParam(value = "purpose", required = false)String purpose,
            @RequestParam(value = "ip", required = false)String ip,
            @RequestParam(value = "hostname", required = false)String hostname,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize){
        Map<String, Object> params = new HashMap<>();
        if(Strings.isNotEmpty(ip)){
            params.put("ip", ip);
        }
        if(Strings.isNotEmpty(purpose)){
            params.put("purpose", purpose);
        }

        if (Strings.isNotEmpty(hostname)){
            params.put("hostname", hostname);
        }
        pageNum = pageNum-1;
        List<EcsResource> resourceList = ecsResourceService.find(params, pageNum, pageSize);

        ResultVo resultVo = new ResultVo();resultVo.setCode(200);
        resultVo.setMsg("查询服务器列表成功！");

        Map map = new HashMap();
        map.put("count", ecsResourceService.count(params));
        map.put("itmes", resourceList);

        resultVo.setData(map);

        return resultVo;
    }

    @ApiOperation(value = "添加服务器", httpMethod = "POST")
    @PostMapping(value = "/addEcs")
    public ResultVo addEcs(
            @Validated EcsResource ecsResource
    ) {
        return ecsResourceService.add(ecsResource);
    }

    @ApiOperation(value = "更新服务器信息", httpMethod = "POST")
    @PostMapping(value = "/updateEcs")
    public ResultVo updateEcs(
            @Validated EcsResource ecsResource) {
        return ecsResourceService.update(ecsResource);
    }

    @ApiOperation(value = "删除服务器", httpMethod = "DELETE")
    @PostMapping(value = "/deleteEcs")
    public ResultVo deleteEcs(
            @RequestParam(value = "id") Long id,
            @RequestParam(value = "password") String password
    ) {
        if (Strings.isEmpty(password)) return ResultVo.renderErr(CodeEnum.ERR).withRemark("密码不能为空");
        return ecsResourceService.delete(id, password);
    }

    @ApiOperation(value = "查询服务器详情", httpMethod = "GET")
    @GetMapping(value = "/detailEcs")
    public ResultVo detailEcs(
            @RequestParam(value = "id") Long id){
        return ecsResourceService.findDetail(id);
    }

    @ApiOperation(value = "校验密码", httpMethod = "POST")
    @PostMapping(value = "/verifyPassword")
    public ResultVo verifyPassword(
            @RequestParam(value = "ip")String ip,
            @RequestParam(value = "password")String password,
            @RequestParam(value = "remotePort", required = false, defaultValue = "22")Integer remotePort
    ){

        ResultVo resultVo = new ResultVo();
        if (!LinuxUtil.ipV4Verify(ip)){
            resultVo.setCode(201);
            resultVo.setMsg("ip格式非法！");
            return resultVo;
        }
        boolean b = ecsResourceService.verify(ip, password,remotePort);
        if (b){
            resultVo.setCode(200);
            resultVo.setMsg("密码正确！");
            return resultVo;
        }
        resultVo.setMsg("密码错误！");
        resultVo.setCode(201);
        return resultVo;
    }


    @ApiOperation("进程列表")
    @GetMapping("/processList")
    public ResultVo processList(String ip){
        return ecsResourceService.processList(ip);
    }

    @ApiOperation("磁盘信息")
    @GetMapping("/disk")
    public ResultVo disk(String ip){
        return ecsResourceService.diskInfo(ip);
    }

    @ApiOperation("cpu信息")
    @GetMapping("/cpuRange")
    public ResultVo cpu(@RequestParam(required = true) String ip,
                        @RequestParam(value = "type", defaultValue = "DAY", required = false) String type){
        if (!RangeTypeEnum.contain(type)){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("类型出错，允许值DAY, WEEK, MONTH");
        }
        return ecsResourceService.cpuInfo(ip, type);
    }

    @ApiOperation("内存历史信息")
    @GetMapping("/memoryRange")
    public ResultVo memory(@RequestParam(required = true) String ip,
                           @RequestParam(value = "type", defaultValue = "DAY", required = false) String type){
        if (!RangeTypeEnum.contain(type)){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("类型出错，允许值DAY, WEEK, MONTH");
        }
        return ecsResourceService.memoryInfo(ip, type);
    }

    @ApiOperation("网卡信息")
    @GetMapping("/ethInfo")
    public ResultVo ethInfo(@RequestParam(required = true) String ip){
        return ecsResourceService.ethInfo(ip);
    }
}
