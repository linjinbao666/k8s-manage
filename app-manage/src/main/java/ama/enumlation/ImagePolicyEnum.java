package ama.enumlation;

/**
 * 镜像拉取策略枚举类
 */
public enum  ImagePolicyEnum {
    Never,IfNotPresent,Always;

    public static boolean contain(String type){
        for (ImagePolicyEnum imagePolicyEnum : ImagePolicyEnum.values()){
            if (imagePolicyEnum.name().equals(type)) return true;
        }
        return false;
    }
}
