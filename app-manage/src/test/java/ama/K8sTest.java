package ama;

import ama.enumlation.CodeEnum;
import ama.exception.BizException;
import ama.vo.AppPodVo;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta2.HorizontalPodAutoscalerBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta2.MetricSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.*;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.apache.commons.io.IOUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class K8sTest {

    public static void main(String[] args) throws IOException {

        File file = ResourceUtils.getFile("D:\\code\\app-manage\\src\\main\\resources\\k8s\\admin-local.conf");
        FileInputStream inputStreamn = new FileInputStream(file);
        String adminConfData = IOUtils.toString(inputStreamn, "UTF-8");
        KubernetesClient client = new DefaultKubernetesClient(Config.fromKubeconfig(adminConfData));
//        HorizontalPodAutoscaler horizontalPodAutoscaler = new HorizontalPodAutoscalerBuilder()
//                .withNewMetadata().withName("nginx2").withNamespace("fline").endMetadata()
//                .withNewSpec()
//                .withNewScaleTargetRef()
//                .withApiVersion("apps/v1")
//                .withKind("Deployment")
//                .withName("nginx")
//                .endScaleTargetRef()
//                .withMinReplicas(1)
//                .withMaxReplicas(10)
//                .addToMetrics(new MetricSpecBuilder()
//                        .withType("Resource")
//                        .withNewResource()
//                        .withName("cpu")
//                        .withNewTarget()
//                        .withType("Utilization")
//                        .withAverageUtilization(50)
//                        .endTarget()
//                        .endResource()
//                        .build())
//                .withNewBehavior()
//                .withNewScaleDown()
//                .addNewPolicy()
//                .withType("Pods")
//                .withValue(4)
//                .withPeriodSeconds(60)
//                .endPolicy()
//                .addNewPolicy()
//                .withType("Percent")
//                .withValue(10)
//                .withPeriodSeconds(60)
//                .endPolicy()
//                .endScaleDown()
//                .endBehavior()
//                .endSpec()
//                .build();
//
//        client.autoscaling().v2beta2().horizontalPodAutoscalers().inNamespace("fline").create(horizontalPodAutoscaler);

//        List<Ingress> items = client.extensions().ingresses().list().getItems();
//        Ingress ingress = client.extensions().ingresses().with.withName("ingress-fline").get();
//        for (int i = 0; i< items.size(); i++){
//            System.out.println(items.get(i));
//        }
//        Optional<Ingress> any = client.extensions().ingresses().list().getItems().stream().filter(tmp -> tmp.getMetadata().getName().equals("ingress-fline")).findAny();
//        System.out.println(any.get());

//        Ingress ingress = client.extensions().ingresses().inAnyNamespace().list().getItems().stream().filter(tmp -> tmp.getMetadata().getName().equals("ingress-fline")).findAny().get();
//        IngressRule ingressRule = ingress.getSpec().getRules().get(0);
//        List<HTTPIngressPath> paths = ingressRule.getHttp().getPaths();
//        boolean b = paths.stream().anyMatch(tmp -> tmp.getPath().equalsIgnoreCase("a-b"));
//        if (b) throw new BizException(CodeEnum.INGRESS_ERR);
//        HTTPIngressPath path = new HTTPIngressPathBuilder().withPath("/"+"a-b")
//                .withBackend(new IngressBackendBuilder().withServiceName("a-b").withServicePort(new IntOrString(8080)).build()).build();
//        paths.add(path);
//        ingressRule.getHttp().setPaths(paths);
//        ingress.getSpec().getRules().set(0, ingressRule);
         io.fabric8.kubernetes.api.model.Service service = client.services()
                 .inNamespace("ingress-nginx")
                 .withName("ingress-nginx-controller").get();
         List<ServicePort> ports = service.getSpec().getPorts();
         System.out.println(ports);
         ServicePort httpPort = ports.stream().filter(tmp -> tmp.getPort().equals(80)).findAny().get();
        Integer nodePort = httpPort.getNodePort();
        System.out.println(httpPort.getNodePort());
         System.out.println(nodePort);

//        client.extensions().ingresses().withName("ingress-fline").get().
//        client.extensions()
//        client.extensions().ingresses().inAnyNamespace().wit
//        client.extensions().ingresses().inNamespace("fline").withName("ingress-fline").createOrReplace(ingress);

//        Map<String, Quantity> hard = client.resourceQuotas()
//                .inNamespace("fline")
//                .withName("fline")
//                .get()
//                .getSpec()
//                .getHard();
//
//        Quantity cpuQuantity = hard.get("cpu");
//        System.out.println(cpuQuantity.getAmount());
//
//        hard.forEach((key, value)->{
//            System.out.println(key+value);
//        });


//        PodResource<Pod, DoneablePod> aDefault = client.pods().inNamespace("default").withName("web-tomcat-7b57d6f9fb-h2pns");

//        System.out.println(aDefault.get);

//        List<Pod> fline = client.pods().inNamespace("fline")
//                .withLabel("name", "vcode2").list().getItems();
//        fline.forEach(pod ->{
//            AppPodVo vo  = new AppPodVo();
//            vo.setPodName(pod.getMetadata().getName());
//            vo.setStartTime(pod.getStatus().getStartTime());
//            vo.setCreatedTime(pod.getMetadata().getCreationTimestamp());
//            vo.setIp(pod.getStatus().getHostIP());
//            vo.setRestartCount(pod.getStatus().getContainerStatuses().get(0).getRestartCount());
//            vo.setStatus(pod.getStatus().getPhase());
//            System.out.println(vo);
//        });


//
//       client.events().inNamespace("default").watch(new Watcher<Event>() {
//           @Override
//           public void eventReceived(Action action, Event resource) {
//               System.out.println("event " + action.name() + " " + resource.toString());
//           }
//
//           @Override
//           public void onClose(KubernetesClientException cause) {
//               System.out.println("Watcher close due to " + cause);
//           }
//           });

       }
}
