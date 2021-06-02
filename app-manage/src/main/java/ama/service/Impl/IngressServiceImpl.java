package ama.service.Impl;

import ama.entity.App;
import ama.enumlation.CodeEnum;
import ama.exception.BizException;
import ama.service.IngressService;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.extensions.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class IngressServiceImpl implements IngressService {

    @Autowired
    KubernetesClient kubernetesClient;
    @Override
    public void addOneIng(App app) {
        Ingress ingress = kubernetesClient.extensions().ingresses().inAnyNamespace().list().getItems().stream().filter(tmp -> tmp.getMetadata().getName().equals("ingress-fline")).findAny().orElse(null);
        if (null == ingress) {
            log.info("系统未部署ing组件，请联系运维处理");
            return;
        }
        IngressRule ingressRule = ingress.getSpec().getRules().get(0);
        List<HTTPIngressPath> paths = ingressRule.getHttp().getPaths();
        boolean b = paths.stream().anyMatch(tmp -> tmp.getPath().equalsIgnoreCase("/"+app.getAppName()));
        if (b) {
            if(paths.stream().anyMatch(tmp -> tmp.getBackend().getServiceName().equalsIgnoreCase(app.getAppName()))) { return; }
            else { throw new BizException(CodeEnum.INGRESS_ERR); }
        }
        HTTPIngressPath path = new HTTPIngressPathBuilder().withPath("/"+app.getAppName()).withBackend(new IngressBackendBuilder().withServiceName(app.getAppName()).withServicePort(new IntOrString(app.getContainerPort())).build()).build();
        paths.add(path);
        ingressRule.getHttp().setPaths(paths);
        ingress.getSpec().getRules().set(0, ingressRule);
        kubernetesClient.extensions().ingresses().inNamespace("fline").withName("ingress-fline").createOrReplace(ingress);
    }

    @Override
    public void delOneIng(App app) {
        Ingress ingress = kubernetesClient.extensions().ingresses().inAnyNamespace().list().getItems().stream().filter(tmp -> tmp.getMetadata().getName().equals("ingress-fline")).findAny().orElse(null);
        if (null == ingress) {
            log.info("系统未部署ing组件，请联系运维处理");
            return;
        }
        IngressRule ingressRule = ingress.getSpec().getRules().get(0);
        List<HTTPIngressPath> paths = ingressRule.getHttp().getPaths();
        HTTPIngressPath path = new HTTPIngressPathBuilder().withPath("/"+app.getAppName()).withBackend(new IngressBackendBuilder().withServiceName(app.getAppName()).withServicePort(new IntOrString(app.getContainerPort())).build()).build();
        paths.remove(path);
        ingressRule.getHttp().setPaths(paths);
        ingress.getSpec().getRules().set(0, ingressRule);
        kubernetesClient.extensions().ingresses().inNamespace("fline").withName("ingress-fline").createOrReplace(ingress);
    }

    @Override
    public int ingressHttpPort() {
        io.fabric8.kubernetes.api.model.Service service = kubernetesClient.services().inNamespace("ingress-nginx").withName("ingress-nginx-controller").get();
        List<ServicePort> ports = service.getSpec().getPorts();
        ServicePort httpPort = ports.stream().filter(tmp -> tmp.getPort().equals(80)).findAny().get();
        Integer nodePort = httpPort.getNodePort();
        return nodePort;
    }

    @Override
    public String findOneIng(App app) {
        Ingress ingress = kubernetesClient.extensions().ingresses().inAnyNamespace().list().getItems().stream().filter(tmp -> tmp.getMetadata().getName().equals("ingress-fline")).findAny().orElse(null);
        if (null == ingress) {
            log.info("系统未部署ing组件，请联系运维处理");
            return null;
        }
        IngressRule ingressRule = ingress.getSpec().getRules().get(0);
        List<HTTPIngressPath> paths = ingressRule.getHttp().getPaths();
        HTTPIngressPath path = paths.stream().filter(tmp -> tmp.getBackend().getServiceName().equalsIgnoreCase(app.getAppName())).findAny().orElse(null);

        return null != path ? path.getPath() : null;
    }
}
