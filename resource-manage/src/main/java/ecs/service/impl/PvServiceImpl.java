package ecs.service.impl;

import ecs.dao.PvDao;
import ecs.entity.Pv;
import ecs.service.PvService;
import ecs.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PvServiceImpl implements PvService {

    public static final String PVC_STATUS="Bound";

    public static final String RWM="ReadWriteMany";
    public static final String ROM="ReadOnlyMany";
    public static final String RWO="ReadWriteOnce";
    public static final String RT="Retain";
    public static final String RC="Recycle";
    public static final String DL="Delete";
    public static final String RU="RollingUpdate";
    public static final String REC="Recreate";
    public static final String AL="Always";
    public static final String AVL="Available";
    public static final String REL="Released";

    @Autowired
    private PvDao pvDao;

    private ResultVo resultVo = new ResultVo();

    @Override
    public List<Pv> findAll(){
        return pvDao.findAll();
    }

    @Override
    public List<Pv> findVolumes(Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<Pv> pageObject = pvDao.findAll(pageable);
        List<Pv> pvList = new ArrayList<>();
        pageObject.getContent().forEach(tmp -> {
            pvList.add(tmp);
        });
        return pvList;
    }

    @Override
    public ResultVo createPv(Pv pv) {

      return null;
    }


    public String getChineseName(String message) throws Exception {

        if(message.equalsIgnoreCase(RWM)) {
            message="多节点读写";
        }else if(message.equalsIgnoreCase(ROM)) {
            message="多节点只读";
        }else if(message.equalsIgnoreCase(RWO)) {
            message="单节点读写";
        }else if(message.equalsIgnoreCase(RT)) {
            message="保留";
        }else if(message.equalsIgnoreCase(RC)) {
            message="回收";
        }else if(message.equalsIgnoreCase(DL)) {
            message="删除";
        }else if(message.equalsIgnoreCase(REL)) {
            message="释放";
        }else if(message.equalsIgnoreCase(PVC_STATUS)) {
            message="绑定";
        }else if(message.equalsIgnoreCase(AVL)) {
            message="空闲";
        }
        return message;
    }
}
