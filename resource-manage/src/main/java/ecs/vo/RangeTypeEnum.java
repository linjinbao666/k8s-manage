package ecs.vo;

/**
 *
 */
public enum RangeTypeEnum {
    DAY,WEEK,MONTH;
    public static boolean contain(String type){
        for (RangeTypeEnum rangeTypeEnum : RangeTypeEnum.values()){
            if (rangeTypeEnum.name().equals(type)) return true;
        }
        return false;
    }
}
