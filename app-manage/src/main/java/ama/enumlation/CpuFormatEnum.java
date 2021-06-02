package ama.enumlation;

/**
 * cpu格式
 */
public enum  CpuFormatEnum {
    M,N;

    public static boolean contain(String type){
        for (CpuFormatEnum cpuFormatEnum : CpuFormatEnum.values()){
            if (cpuFormatEnum.name().equals(type)) return true;
        }
        return false;
    }
}
