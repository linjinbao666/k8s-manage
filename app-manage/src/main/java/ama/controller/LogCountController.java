package ama.controller;


import ama.service.LogService;
import ama.vo.ResultVo;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.ParseException;

@Api("日志分析接口")
@RestController
@RequestMapping("/logCount")
public class LogCountController {

    @Autowired
    LogService logService;
    /**
     * 统计概览 顶部几个指标
     * @return
     */
    @ApiOperation("概况统计")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace", value = "名称空间，和用户信息绑定")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功")
    })
    @GetMapping("/summary")
    public ResultVo summary(@RequestParam(value = "namespace", required = false) String namespace) throws IOException {
        return logService.conutSummary(namespace);
    }

    /**
     * 根据时间统计
     * @param namespace
     * @param date
     * @param startTime
     * @param endTime
     * @param labels
     * @return
     * @throws IOException
     * @throws ParseException
     */
    @ApiOperation("根据时间查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace", value = "名称空间，和用户信息绑定"),
            @ApiImplicitParam(name = "appName", value = "应用名称", defaultValue = "appName"),
            @ApiImplicitParam(name = "date", value = "日期", defaultValue = "day"),
            @ApiImplicitParam(name = "startTime", value = "开始时间"),
            @ApiImplicitParam(name = "endTime", value = "结束时间"),
            @ApiImplicitParam(name = "labels", value = "关键字查询")

    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功")
    })
    @GetMapping("/countByTime")
    public ResultVo countByTime(
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "appName", required = false)String appName,
            @RequestParam(value = "date", defaultValue = "day")String date,
            @RequestParam(value = "startTime", required = false)String startTime,
            @RequestParam(value = "endTime", required = false)String endTime,
            @RequestParam(value = "labels", required = false)String labels) throws IOException, ParseException {

        return logService.countByTime(namespace,date, startTime, endTime, labels,appName);
    }


    @ApiOperation("统计应用日志在副本中的分布")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace", value = "名称空间，和用户信息绑定")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "按照应用查询成功")
    })
    @GetMapping("/countByContainer")
    public ResultVo countByContainer(@RequestParam(value = "namespace", required = false) String namespace) throws IOException {
        return logService.countByApp(namespace);
    }
}
