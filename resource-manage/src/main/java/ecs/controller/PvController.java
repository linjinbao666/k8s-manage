package ecs.controller;

import ecs.entity.Pv;
import ecs.service.PvService;
import ecs.vo.ResultVo;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台使用动态pv，无需手动创建pv
 */
@Api(tags = "pv接口")
@RestController
@RequestMapping(value = "/persistenVolume")
public class PvController {

    @Autowired
    private PvService pvService;
    @GetMapping(value = "/findPersistentVolumes")
    public ResultVo findPersistentVolumes(@RequestParam(value = "pageNum", required = false, defaultValue = "1")Integer pageNum,
                                          @RequestParam(value = "pageSize", required = false, defaultValue = "5")Integer pageSize
    ){
        List<Pv> list = pvService.findVolumes(pageNum,pageSize);
        ResultVo resultVo = new ResultVo();
        resultVo.setCode(200);
        resultVo.setMsg("查询成功");
        return resultVo;
    }

    @PostMapping(value = "/createPv")
    public ResultVo createPv(@RequestParam(value = "pvName")String pvName,
                             @RequestParam(value = "capaCity")Integer capaCity,
                             @RequestParam(value = "accessModes")String accessModes,
                             @RequestParam(value = "reclaimPolicy")String reclaimPolicy,
                             @RequestParam(value = "externalMountPath")String externalMountPath){

        Pv pv = new Pv();
        pv.setAccessModes(accessModes);
        pv.setPvcName(pvName);
        pv.setCapaCity(capaCity);
        pv.setExternalMountPath(externalMountPath);
        pv.setReclaimPolicy(reclaimPolicy);

        ResultVo resultVo = pvService.createPv(pv);

        return resultVo;
    }

    @PostMapping(value = "/deletePv")
    public ResultVo deletePv(@RequestParam(value = "id")Long id){

        return null;
    }

    @GetMapping(value = "/findVolumesInfo")
    public ResultVo findVolumesInfo(@RequestParam(value = "id")Long id){

        return null;
    }

    @GetMapping(value = "/findFilesInVolumes")
    public ResultVo findFilesInVolumes(){
        return null;
    }

    public ResultVo uploadFile(){

        return null;
    }

    public ResultVo deleteFilesInPv(){
        return null;
    }
}
