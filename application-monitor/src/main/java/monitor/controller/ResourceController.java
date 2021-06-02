package monitor.controller;

import io.swagger.annotations.*;
import monitor.enumlation.CodeEnum;
import monitor.enumlation.TimeEnum;
import monitor.service.ResourceService;
import monitor.vo.K8sResourceVo;
import monitor.vo.ResultVo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api("资源监控")
@RestController
@RequestMapping("/resource")
public class ResourceController {

    @Autowired
    ResourceService resourceService;

    @ApiOperation("查询cpu和内存使用")
    @ApiResponse(code = 200, message = "查询cpu和内存总量成功")
    @GetMapping("/cpuAndMemory")
    public ResultVo resuource(){
        List<K8sResourceVo> allCpuAndMemory = resourceService.allCpuAndMemory();
        return ResultVo.renderOk(allCpuAndMemory).withRemark("查询cpu和内存总量成功！");
    }

    @ApiOperation("查询cpu使用率")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appName", value = "应用名称"),
            @ApiImplicitParam(name = "containerName", value = "容器名称"),
            @ApiImplicitParam(name = "chronoUnit", value = "时间类型，可选值,DAY,WEEK,MONTH")
    })
    @ApiResponse(code = 200, message = "查询cpu使用率成功")
    @GetMapping("/cpuRange")
    public ResultVo cpuRange(
            @RequestParam(value = "appName",required = false)String appName,
            @RequestParam(value = "containerName", required = false)String containerName,
            @RequestParam(value = "chronoUnit", required = false)String chronoUnit
    ){
        if (Strings.isEmpty(chronoUnit)) chronoUnit = "DAY";
        if (!TimeEnum.contain(chronoUnit)) return ResultVo.renderOk().withRemark("时间格式必须为DAY, WEEK, MONTH");

        if (Strings.isEmpty(appName)) appName = "";
        if (Strings.isEmpty(containerName)) containerName = "";
        return resourceService.getCpuRange(appName, containerName, chronoUnit);
    }

    @ApiOperation("查询内存使用率")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appName", value = "应用名称"),
            @ApiImplicitParam(name = "containerName", value = "容器名称"),
            @ApiImplicitParam(name = "chronoUnit", value = "时间类型，可选值,DAY,WEEK,MONTH")
    })
    @ApiResponse(code = 200, message = "查询内存使用率成功")
    @GetMapping("/memoryRange")
    public ResultVo memoryRange(
            @RequestParam(value = "appName", required = false)String appName,
            @RequestParam(value = "containerName", required = false)String containerName,
            @RequestParam(value = "chronoUnit", required = false)String chronoUnit
    ){
        if (Strings.isEmpty(chronoUnit)) chronoUnit = "DAY";
        if (!TimeEnum.contain(chronoUnit)) return ResultVo.renderOk().withRemark("时间格式必须为DAY, WEEK, MONTH");
        if (Strings.isEmpty(appName)) appName = "";
        if (Strings.isEmpty(containerName)) containerName = "";
        return resourceService.getMemoryRange(appName, containerName, chronoUnit);
    }

    @ApiOperation("查询io使用率")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appName", value = "应用名称"),
            @ApiImplicitParam(name = "containerName", value = "容器名称"),
            @ApiImplicitParam(name = "chronoUnit", value = "时间类型，可选值,DAY,WEEK,MONTH")
    })
    @ApiResponse(code = 200, message = "查询io使用率成功")
    @GetMapping("/ioRange")
    public ResultVo ioRange(
            @RequestParam(value = "appName",required = false)String appName,
            @RequestParam(value = "containerName", required = false)String containerName,
            @RequestParam(value = "chronoUnit", required = false)String chronoUnit
    ){
        if (Strings.isEmpty(chronoUnit)) chronoUnit = "DAY";
        if (!TimeEnum.contain(chronoUnit)) return ResultVo.renderOk().withRemark("时间格式必须为DAY, WEEK, MONTH");
        if (Strings.isEmpty(appName)) appName = "";
        if (Strings.isEmpty(containerName)) containerName = "";
        return resourceService.getIORange(appName, containerName, chronoUnit);
    }


    @ApiOperation("查询所有应用")
    @ApiResponse(code = 200, message = "查询应用成功！")
    @GetMapping("/apps")
    public ResultVo apps(){
        return resourceService.apps();
    }


    @GetMapping("/apps2")
    public ResultVo apps2(){
        return resourceService.apps2();
    }

    @ApiOperation("查询容器")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace", value = "名称空间"),
            @ApiImplicitParam(name = "appName", value = "应用名称"),
    })
    @ApiResponse(code = 200, message = "查询容器成功！")
    @GetMapping("/containers")
    public ResultVo containers(
            @RequestParam("namespace") String namespace,
            @RequestParam("appName") String appName){
        if (Strings.isEmpty(namespace) || Strings.isEmpty(appName)){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("参数不为空");
        }
        return resourceService.containers(namespace, appName);
    }


}
