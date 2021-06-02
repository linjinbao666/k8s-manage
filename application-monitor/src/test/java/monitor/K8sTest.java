package monitor;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionSpec;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta2.HorizontalPodAutoscalerBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta2.MetricSpecBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.apache.commons.io.IOUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

public class K8sTest {

    public static void main(String[] args) throws IOException {

        File file = ResourceUtils.getFile("classpath:k8s/admin.conf");
        FileInputStream inputStreamn = new FileInputStream(file);
        String adminConfData = IOUtils.toString(inputStreamn, "UTF-8");
        KubernetesClient client = new DefaultKubernetesClient(Config.fromKubeconfig(adminConfData));

        String RULE_DEF =
                "apiVersion: monitoring.coreos.com/v1\n" +
                        "kind: PrometheusRule\n" +
                        "metadata:\n" +
                        "  labels:\n" +
                        "  name: rule\n" +
                        "  namespace: fline\n" +
                        "spec:\n" +
                        "  groups:\n" +
                        "  - name: test-prometheus-scraper5\n" +
                        "    rules:\n" +
                        "      - alert: Prometheus scraping errors\n" +
                        "        expr: >-\n" +
                        "          count(up == 0)>0\n" +
                        "        for: 5m\n" +
                        "        labels:\n" +
                        "          page: test_page_id1\n" +
                        "          team: myteam\n" +
                        "        annotations:\n" +
                        "          summary: \"Prometheus Scraping has been Failed 05\"\n" +
                        "          description: |\n" +
                        "            Check failing services";

        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                .withGroup("monitoring.coreos.com")
                .withPlural("prometheusrules")
                .withScope("Namespaced")
                .withVersion("v1")
                .build();
        client.customResource(crdContext).create("fline",
                RULE_DEF);



//        ObjectMeta metadata = new ObjectMetaBuilder()
//                .withLabels(new HashMap<String, String>(){{put("prometheus","k8s");}})
//                .withLabels(new HashMap<String, String>(){{put("role","alert-rules");}})
//                .withName("test").build();
//
//        String group = "- name: test-rules\r\n" +
//                "  rules:\r\n" +
//                "  - alert: " + "KubeletDown" + "\r\n" +
//                "    expr: " + "absent(up{job=\"kubelet\"} == 1)" + "\r\n" +
//                "    for: 2m\r\n" +
//                "    labels:\r\n" +
//                "      team: node\r\n" +
//                "    annotations:\r\n" +
//                "      message: \"{{$labels.instance}}: " + "Kubelet has disappeared from Prometheus target discovery" +"\"\r\n" ;
//
//        CustomResourceDefinitionSpec spec = new CustomResourceDefinitionBuilder()
//                .withNewSpec()
//                .withNewGroup(group)
//                .endSpec()
//                .buildSpec();
//
//        CustomResourceDefinition prometheousRuleCrd = new CustomResourceDefinitionBuilder()
//                .withApiVersion("monitoring.coreos.com/v1")
//                .withKind("PrometheusRule")
//                .withMetadata(metadata)
//                .withSpec(spec)
//                .build();
//
//        client.customResourceDefinitions().create(prometheousRuleCrd);
//
//        client.customResource(crdContext).create()

//        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
//                .withGroup("monitoring.coreos.com")
//                .withPlural("prometheusrules")
//                .withScope("Namespaced")
//                .withVersion("v1")
//                .build();
//
//        client.customResource(crdContext).create


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
//


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
