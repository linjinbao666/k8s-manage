package ama.controller;

import ama.entity.App;
import ama.enumlation.ImagePolicyEnum;
import ama.enumlation.ServiceTypeEnum;
import ama.enumlation.UpdatePolicyEnum;
import ama.service.AppService;
import ama.util.MyUtil;
import ama.enumlation.CodeEnum;
import ama.vo.ResultVo;
import ama.vo.UserContext;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.*;
import org.apache.commons.io.IOUtils;
import org.apache.directory.api.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Api(value = "应用接口")
@RestController
@RequestMapping(value = "/app")
public class AppController {

    @Autowired
    AppService appService;
    @Autowired
    RestTemplate restTemplate;

    @Value("${approval.gateway}")
    private String gateway;

    @ApiOperation("获取所有应用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appName",value = "应用名称", example = "vcode"),
            @ApiImplicitParam(name = "pageNum",value = "页面", example = "1", defaultValue = "1"),
            @ApiImplicitParam(name = "pageSize",value = "分页大小", example = "5", defaultValue = "5"),
    })
    @ApiResponses({@ApiResponse(code = 200, message = "获取所有应用")})
    @GetMapping("/")
    public ResultVo app(
            @RequestParam(value = "appName", required = false)String appName,
            @RequestParam(value = "pageNum",required = false, defaultValue = "1")Integer pageNum,
            @RequestParam(value = "pageSize",required = false, defaultValue = "5")Integer pageSize
    ){
        Map params = new HashMap();
        if (Strings.isNotEmpty(appName)) params.put("appName", appName);
        List items =  appService.findAll(params, pageNum-1, pageSize);
        long count = appService.count(params);
        return ResultVo.renderOk(new HashMap<String, Object>(){{
            put("items", items);
            put("count", count);
        }}).withRemark("获取所有应用");
    }

    @ApiOperation(("查询应用详情"))
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace", required = true, value = "名称空间"),
            @ApiImplicitParam(name = "appName", required = true, value = "应用名称")
    })
    @ApiResponse(code = 200, message = "查询应用详情")
    @GetMapping("/{appName}")
    public ResultVo app(
        @PathVariable(name = "appName")String appName,
        @RequestParam(name = "namespace")String namespace
    ){
        App app = appService.findOne(namespace, appName);
        if (null == app) return ResultVo.renderErr().withRemark("相关应用不存在" + appName);
        return ResultVo.renderOk(app).withRemark("查询应用详情");
    }

    @ApiOperation("同步名称空间下所有应用到数据库")
    @ApiImplicitParams({@ApiImplicitParam(name = "namespace", value = "名称空间",example = "fline", required = true)})
    @ApiResponses({@ApiResponse(code = 200, message = "同步成功！")})
    @GetMapping("/sync")
    public ResultVo syncApp(String namespace){
        appService.syncApp(namespace);
        return ResultVo.renderOk().withRemark("同步成功！");
    }

    @ApiOperation("发布应用")
    @PostMapping("/publish")
    public ResultVo publishApp(@Validated App app) {
        if (Strings.isEmpty(app.getNamespace())) {
            String namespace = MyUtil.namespace(restTemplate, gateway);
            app.setNamespace(namespace);
        }
        String valid = validApp(app);
        if (null != valid) { return ResultVo.renderErr(CodeEnum.ERR).withRemark(valid); }
        if (Strings.isEmpty(app.getCnName())){ app.setCnName(app.getAppName()); }
        app.setCpuFormat("m");
        app.setMemoryFormat("Mi");
        return appService.addOne(app);
    }

    @ApiOperation("获取存储卷列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace", value = "名称空间", required = true)
    })
    @GetMapping("/pvcs")
    public ResultVo pvcs(@RequestParam(value = "namespace", required = false) String namespace){
        return appService.pvcs(namespace);
    }


    @ApiOperation("更新发布的应用")
    @PostMapping("/update")
    public ResultVo updateApp(App app) throws Exception {
        System.out.println("app:"+app);
        String valid = validApp(app);
        if (null != valid) { return ResultVo.renderErr(CodeEnum.ERR).withRemark(valid); }
        if (Strings.isEmpty(app.getCnName())){ app.setCnName(app.getAppName()); }
        app.setCpuFormat("m");
        app.setMemoryFormat("Mi");
        return appService.updateOne(app);
    }

    /**
     * 停止应用
     * @param namespace
     * @param appName
     * @return
     */
    @ApiOperation("停止应用，即数据库保留，服务器删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace",value = "名称空间", required = true),
            @ApiImplicitParam(name = "appName",value = "应用名称", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 201, message = "数据库不存在该应用"),
            @ApiResponse(code = 201, message = "停止失败"),
            @ApiResponse(code = 200, message = "停止成功"),
    })
    @PostMapping("/pause")
    public ResultVo pauseApp(String namespace, String appName){
        return appService.pauseOne(namespace, appName);
    }

    @ApiOperation("重启应用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace",value = "名称空间", required = true),
            @ApiImplicitParam(name = "appName",value = "应用名称", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 201, message = "数据库不存在该应用"),
            @ApiResponse(code = 201, message = "重启失败"),
            @ApiResponse(code = 200, message = "重启成功"),
    })
    @PostMapping("/restart")
    public ResultVo restart(String namespace, String appName){
        return appService.restart(namespace, appName);
    }

    @ApiOperation("删除应用，即数据库删除，服务器删除")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "名称空间",name = "namespace" ,required = true),
            @ApiImplicitParam(value = "应用名称",name = "appName", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 201, message = "数据库不存在该应用"),
            @ApiResponse(code = 201, message = "删除失败"),
            @ApiResponse(code = 200, message = "删除成功"),
    })
    @DeleteMapping("/delete")
    public ResultVo deleteApp(String namespace, String appName){
        if (Strings.isEmpty(namespace) || Strings.isEmpty(appName)) {
            return ResultVo.renderErr().withRemark("删除参数不允许为空");
        }
        return appService.deleteOne(namespace, appName);
    }

    @ApiOperation("获取数据库保存的已经使用的端口，不保证全")
    @ApiResponse(code = 200, message = "已经使用的端口不可再次使用")
    @GetMapping("/usedPorts")
    public ResultVo usedPorts(){
        return ResultVo.renderOk(appService.usedNodePorts()).withRemark("已经使用的端口不可再次使用");
    }

    @ApiOperation("查询节点标签")
    @GetMapping("/nodeSelectors")
    public ResultVo nodeSelectors(){
        Map map = appService.nodeSelectors();
        return ResultVo.renderOk(map).withRemark("查询节点标签完成");
    }

    @ApiOperation("获取指定应用下的副本信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace",value = "名称空间", required = true),
            @ApiImplicitParam(name = "appName",value = "应用名称", required = true)
    })
    @ApiResponse(code = 200, message = "查询应用副本成功！")
    @GetMapping("/pods")
    public ResultVo pods(String namespace, String appName){
        return appService.pods(namespace, appName);
    }

    @ApiOperation("删除副本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace", value = "名称空间"),
            @ApiImplicitParam(name = "podName", value = "副本名称")
    })
    @DeleteMapping("/deletePod")
    public ResultVo deletePod(
            String namespace,
            String podName
    ){
        return appService.deletePod(namespace, podName);
    }

    @ApiOperation("导出指定副本的日志")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "namespace", required = true),
            @ApiImplicitParam(value = "副本名称",name ="podName", required = true),
            @ApiImplicitParam(value = "行数",name ="lines", required = true),
    })
    @ApiResponse(code = 200, message = "查询日志成功")
    @GetMapping("/podLog")
    public String podLog(
            HttpServletResponse response,
            String namespace, String podName, Integer lines) throws IOException {
        final ReentrantLock lock = new ReentrantLock();
        if (lock.tryLock()){
            OutputStream outputStream = response.getOutputStream();
            response.setContentType("application/x-download");
            response.addHeader("Content-Disposition", "attachment;filename="
                    + URLEncoder.encode(podName, "UTF-8")+".log");
            InputStream download = null;
            try {
                download = appService.podLog(namespace, podName, lines);
                IOUtils.copy(download, outputStream);
                outputStream.flush();
            }catch (Exception e){
                e.printStackTrace();
                return "导出失败"+e.getMessage();
            }finally {
                download.close();
                lock.unlock();
            }
            return null;
        }

        return "导出操作进行中...";
    }


    @ApiOperation("查询厂商资源使用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace", value = "名称空间", required = true)
    })
    @ApiResponse(code = 200, message = "查询成功")
    @GetMapping(value = "/resourceUsed")
    ResultVo resourceUsed(
            @RequestParam(value = "namespace", required = false) String namespace){
        if (Strings.isEmpty(namespace)) {
            namespace = MyUtil.namespace(restTemplate, gateway);
        }
        return appService.resourceUsed(namespace);
    }

    @ApiOperation("自动扩容")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "namespace", value = "名称空间", required = true),
            @ApiImplicitParam(name = "appName", value = "应用名称", required = true),
            @ApiImplicitParam(name = "min", value = "最小个数", required = true),
            @ApiImplicitParam(name = "max", value = "最大个数", required = true),
            @ApiImplicitParam(name = "cpuPercentage", value = "cpu触发条件", required = true),
            @ApiImplicitParam(name = "memoryPercentage", value = "内存触发条件", required = true),
    })
    @ApiResponse(code = 200, message = "扩容成功")
    @PostMapping("/autoExpansion")
    public ResultVo autoExpansion(
            String namespace,
            String appName,
            Integer min,
            Integer max,
            Integer cpuPercentage,
            Integer memoryPercentage
    ){
        if (min <1) return ResultVo.renderErr().withRemark("应用个数不得少于1个");
        if (min > max) return ResultVo.renderErr().withRemark("最大个数应当大于最小个数");
        return appService.hpa(namespace,appName, min, max, cpuPercentage, memoryPercentage);
    }

    @PostMapping("/delAutoExpansion")
    ResultVo delAutoExpansion(
            @RequestParam(value = "namespace") String namespace,
            @RequestParam(value = "appName") String appName
    ){
        return appService.deleteHpa(namespace, appName);
    }

    @Value("${docker-java.registerUrl}")
    String registerUrl;

    /**
     * app参数校验
     * @param app
     * @return
     */
    private String validApp(App app) {
        if (Strings.isEmpty(app.getAppName()) || Strings.isEmpty(app.getNamespace())) return "厂商空间和应用名称不允许为空";

        if (app.getReplicas()>10 || app.getReplicas()<1) return "副本个数不允许超过10个，不允许低于1个";

        if (!UpdatePolicyEnum.contain(app.getUpdatePolicy())) return "升级策略不存在";

        if (Strings.isEmpty(app.getImageName())) return "镜像名称错误";

//        if(!app.getImageName().contains(registerUrl)) return "为了保证后续正常运行，请使用已经推送到仓库的镜像";

        if (!ImagePolicyEnum.contain(app.getImagePolicy())) return "镜像拉取策略不存在";

        if (!MyUtil.isNumeric(app.getCpuAmount())) return "cpu大小参数错误";

        if (Strings.isNotEmpty(app.getServiceType()) && !ServiceTypeEnum.contain(app.getServiceType())){
            return "端口暴露类型非法";
        }else if (null ==app.getNodePort() || app.getNodePort() ==0){

        }else if (app.getNodePort()<30000 || app.getNodePort()>60034) {
            return "端口必须在30000到60034之间";
        }else if(appService.usedNodePorts().contains(app.getNodePort())){
            return "端口已经已经被占用";
        }

        if ((!MyUtil.isNumeric(app.getMemoryAmount()))) return "内存大小参数错误";

        if (Strings.isEmpty(app.getCpuAmount()) || Strings.isEmpty(app.getMemoryAmount())){
            return "cpu和内存参数有误";
        }

        return null;
    }

}
