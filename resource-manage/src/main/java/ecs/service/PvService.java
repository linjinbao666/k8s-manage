package ecs.service;

import ecs.entity.Pv;
import ecs.vo.ResultVo;

import java.util.List;

public interface PvService {
    List<Pv> findAll();

    List<Pv> findVolumes(Integer pageNum, Integer pageSize);

    ResultVo createPv(Pv pv);
}
