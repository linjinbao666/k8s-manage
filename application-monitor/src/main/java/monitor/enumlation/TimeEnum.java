package monitor.enumlation;

public enum  TimeEnum {
    DAY,WEEK,MONTH;

    public static boolean contain(String type){
        for (TimeEnum timeEnum : TimeEnum.values()){
            if (timeEnum.name().equals(type)) return true;
        }
        return false;
    }
}
