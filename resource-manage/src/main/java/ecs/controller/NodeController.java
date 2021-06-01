package ecs.controller;

import ecs.entity.KubernetesNode;
import ecs.service.KubernetesNodeService;
import ecs.vo.CodeEnum;
import ecs.vo.K8sResourceVo;
import ecs.vo.ResultVo;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "kubernetes节点接口")
@RestController
@RequestMapping(value = "/kubernetes")
public class NodeController {
    @Autowired
    private KubernetesNodeService kubernetesNodeService;

    @ApiOperation(value = "k8s节点查询", httpMethod = "GET")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询节点成功！"),
            @ApiResponse(code = 201, message = "查询节点列表失败！")
    })
    @GetMapping(value = "/node")
    public ResultVo node(){
        List<KubernetesNode> nodes = kubernetesNodeService.findAll();
        if (nodes.size() == 0){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("查询节点列表失败");
        }
        return ResultVo.renderOk(nodes);
    }

    @ApiOperation(value = "k8s节点详情", httpMethod = "GET")
    @ApiImplicitParam(paramType = "query", name = "nodeName", value = "节点名称", required = true, example = "master", dataType = "String")
    @ApiResponses({
            @ApiResponse(code= 200, message = "查询详情成功！", response = String.class),
            @ApiResponse(code= -1, message = "查询失败", response = String.class)
    })
    @GetMapping(value = "/node/{nodeName}")
    public ResultVo findNodeByHostName(@PathVariable("nodeName") String nodeName){
       return kubernetesNodeService.getNodeDetail(nodeName);
    }

    @ApiOperation(value = "查询总cpu和内存资源")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功！"),
            @ApiResponse(code = 201, message = "查询失败")
    })
    @GetMapping(value = "/cpu")
    public ResultVo getCpuAndMemoryInfo(){
        List<K8sResourceVo> info = kubernetesNodeService.getCpuAndMemoryInfo();
        if (info.isEmpty()){
            return ResultVo.renderErr(CodeEnum.SERVER_ERR);
        }
        return ResultVo.renderOk(info).withRemark("查询总cpu和内存资源成功！");
    }

}
