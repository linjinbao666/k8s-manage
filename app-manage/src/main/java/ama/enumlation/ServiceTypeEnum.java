package ama.enumlation;

/**
 * 服务端口类型，内部端口， ingress暴露， nodeport
 */

public enum  ServiceTypeEnum {
    ClusterIP, NodePort, INGRESS;

    public static boolean contain(String serviceType){
        for (ServiceTypeEnum serviceTypeEnum : ServiceTypeEnum.values()){
            if (serviceTypeEnum.name().equals(serviceType)) return true;
        }
        return false;
    }
}
