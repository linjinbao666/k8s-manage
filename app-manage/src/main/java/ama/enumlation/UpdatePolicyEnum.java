package ama.enumlation;

/**
 * 升级策略枚举类
 */
public enum UpdatePolicyEnum {
    RollingUpdate,Recreate;
    public static boolean contain(String type){
        for (UpdatePolicyEnum updatePolicyEnum : UpdatePolicyEnum.values()){
            if (updatePolicyEnum.name().equals(type)) return true;
        }
        return false;
    }
}
