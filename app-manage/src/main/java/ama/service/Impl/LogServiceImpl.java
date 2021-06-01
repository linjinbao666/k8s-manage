package ama.service.Impl;

import ama.service.LogService;
import ama.vo.ResultVo;
import cn.hutool.json.JSONObject;
import org.apache.directory.api.util.Strings;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilters;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class LogServiceImpl implements LogService {
    @Autowired
    RestHighLevelClient restHighLevelClient;
    @Value("${elasticsearch.index}")
    String index;

    @Override
    public ResultVo conutSummary(String namespace) throws IOException {
        SearchSourceBuilder search = new SearchSourceBuilder();
        search.explain(false).size(0).trackTotalHits(true);
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (namespace != null) boolQuery.should(QueryBuilders.matchQuery("kubernetes.namespace_name", namespace));
        boolQuery.mustNot(QueryBuilders.matchQuery("kubernetes.namespace_name.keyword", "kube-system"));
        boolQuery.mustNot(QueryBuilders.matchQuery("kubernetes.namespace_name.keyword", "default"));
        search.aggregation(AggregationBuilders.terms("hosts").field("kubernetes.host.keyword"))
                .aggregation(AggregationBuilders.terms("by_container_name").field("kubernetes.container_name.keyword"))
                .aggregation(AggregationBuilders.terms("by_pod_name").field("kubernetes.pod_name.keyword"))
                .aggregation(AggregationBuilders.terms("by_hostname").field("hostname.keyword"))
                .aggregation(AggregationBuilders.filters("by_level",
                        new FiltersAggregator.KeyedFilter("errors", QueryBuilders.matchQuery("log","error")),
                        new FiltersAggregator.KeyedFilter("infos", QueryBuilders.matchQuery("log","info")),
                        new FiltersAggregator.KeyedFilter("warns", QueryBuilders.matchQuery("log","warn")),
                        new FiltersAggregator.KeyedFilter("debugs", QueryBuilders.matchQuery("log","debug"))));

        SearchRequest request = new SearchRequest();
        request.source(search).indices(index);
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        ParsedStringTerms by_hostsBuckets = response.getAggregations().get("hosts");
        ParsedStringTerms by_container_nameBuckets = response.getAggregations().get("by_container_name");
        ParsedStringTerms by_pod_nameBuckets = response.getAggregations().get("by_pod_name");
        ParsedStringTerms by_hostnameBuckets = response.getAggregations().get("by_hostname");
        ParsedFilters by_levelBuckets = response.getAggregations().get("by_level");

        Map<String, Long> hosts = new HashMap<String, Long>();
        Map<String, Long> container_names = new HashMap<String, Long>();
        Map<String, Long> pod_names = new HashMap<String, Long>();
        Map<String, Long> hostnames = new HashMap<String, Long>();
        Map<String, Long> levels = new HashMap<String, Long>();
        by_hostsBuckets.getBuckets().forEach(host -> {
            hosts.put(host.getKeyAsString(), host.getDocCount());
        });
        hosts.put("total", (long) by_hostsBuckets.getBuckets().size());

        by_container_nameBuckets.getBuckets().forEach(container_name -> {
            container_names.put(container_name.getKeyAsString(), container_name.getDocCount());
        });
        container_names.put("total", (long) by_container_nameBuckets.getBuckets().size());

        by_pod_nameBuckets.getBuckets().forEach(pod_name -> {
            pod_names.put(pod_name.getKeyAsString(), pod_name.getDocCount());
        });
        pod_names.put("total", (long) by_pod_nameBuckets.getBuckets().size());

        by_hostnameBuckets.getBuckets().forEach(hostname ->{
            hostnames.put(hostname.getKeyAsString(), hostname.getDocCount());
        });
        hostnames.put("total", (long) by_hostnameBuckets.getBuckets().size());

        by_levelBuckets.getBuckets().forEach(level ->{
            levels.put("total", levels.getOrDefault("total", 0l)+level.getDocCount());
            levels.put(level.getKeyAsString(), level.getDocCount());
        });

        JSONObject list = new JSONObject();
        list.put("by_host", hosts);
        list.put("by_container_name", container_names);
        list.put("by_pod_name", pod_names);
        list.put("by_hostname", hostnames);
        list.put("by_level", levels);

        return ResultVo.renderOk(list).withRemark("查询成功");
    }

    @Override
    public ResultVo countByTime(String namespace, String date,
                                String startTime, String endTime, String labels, String appName) throws ParseException, IOException {
        SearchSourceBuilder search = new SearchSourceBuilder();
        search.explain(false).size(0).trackTotalHits(true);
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        if (namespace != null) queryBuilder.should(QueryBuilders.matchQuery("kubernetes.namespace_name", namespace));
        queryBuilder.mustNot(QueryBuilders.matchQuery("kubernetes.namespace_name.keyword", "kube-system"));
        queryBuilder.mustNot(QueryBuilders.matchQuery("kubernetes.namespace_name.keyword", "default"));
        if(!Strings.isEmpty(labels)) {
            String[] labelArray = labels.split(",");
            for(String label : labelArray) {
                queryBuilder.must(QueryBuilders.matchQuery("log", label));
            }
        }
        if (!Strings.isEmpty(appName)){
            queryBuilder.must(QueryBuilders.matchQuery("log", appName));
        }
        search.query(queryBuilder);
        SimpleDateFormat dateiso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        SimpleDateFormat date2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        AggregationBuilder filter_by_level  =  AggregationBuilders.filters("by_level",
                new FiltersAggregator.KeyedFilter("errors", QueryBuilders.matchQuery("log","error")),
                new FiltersAggregator.KeyedFilter("infos", QueryBuilders.matchQuery("log","info")),
                new FiltersAggregator.KeyedFilter("debugs", QueryBuilders.matchQuery("log","debug")),
                new FiltersAggregator.KeyedFilter("warns", QueryBuilders.matchQuery("log","warn")));
        switch(date){
            case "month":
                if(Strings.isEmpty(startTime) || Strings.isEmpty(endTime)) {
                    calendar1.set(Calendar.DAY_OF_YEAR, calendar1.get(Calendar.DAY_OF_YEAR) - 29);
                    startTime = date2.format(calendar1.getTime());
                    calendar2.set(Calendar.DAY_OF_YEAR, calendar2.get(Calendar.DAY_OF_YEAR));
                    endTime = date2.format(calendar2.getTime());
                }
                search.aggregation(AggregationBuilders.dateHistogram("by_month").field("@timestamp")
                        .dateHistogramInterval(DateHistogramInterval.WEEK).format("MM-dd")
                        .subAggregation(filter_by_level));
                break;
            case "week":
                if(Strings.isEmpty(startTime) || Strings.isEmpty(endTime)) {
                    calendar1.set(Calendar.DAY_OF_YEAR, calendar1.get(Calendar.DAY_OF_YEAR) - 7);
                    startTime = date2.format(calendar1.getTime());
                    calendar2.set(Calendar.DAY_OF_YEAR, calendar2.get(Calendar.DAY_OF_YEAR));
                    endTime = date2.format(calendar2.getTime());
                }
                search.aggregation(AggregationBuilders.dateHistogram("by_week").field("@timestamp")
                        .dateHistogramInterval(DateHistogramInterval.DAY).format("dd")
                        .subAggregation(filter_by_level));
                break;
            case "day":
                if(Strings.isEmpty(startTime) || Strings.isEmpty(endTime)) {
                    calendar1.set(Calendar.HOUR_OF_DAY, calendar1.get(Calendar.HOUR_OF_DAY) - 24);
                    startTime = date2.format(calendar1.getTime());
                    calendar2.set(Calendar.HOUR_OF_DAY, calendar2.get(Calendar.HOUR_OF_DAY));
                    endTime = date2.format(calendar2.getTime());
                }
                search.aggregation(AggregationBuilders.dateHistogram("by_day").field("@timestamp")
                        .dateHistogramInterval(DateHistogramInterval.HOUR).format("hh")
                        .subAggregation(filter_by_level));
                break;
            case "hour":
                if(Strings.isEmpty(startTime) || Strings.isEmpty(endTime)) {
                    calendar1.set(Calendar.MINUTE, calendar1.get(Calendar.MINUTE) - 59);
                    startTime = date2.format(calendar1.getTime());
                    calendar2.set(Calendar.MINUTE, calendar2.get(Calendar.MINUTE));
                    endTime = date2.format(calendar2.getTime());
                }
                search.aggregation(AggregationBuilders.dateHistogram("by_hour").field("@timestamp")
                        .dateHistogramInterval(DateHistogramInterval.MINUTE).format("hh-mm")
                        .subAggregation(filter_by_level));
                break;
            default :
                break;
        }
        queryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                .gte(dateiso8601.format(date2.parse(startTime))));
        queryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                .lte(dateiso8601.format(date2.parse(endTime))));
        search.sort(SortBuilders.fieldSort("@timestamp").order(SortOrder.DESC));
        search.query(queryBuilder);
        SearchRequest request = new SearchRequest();
        request.source(search).indices(index);

        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        ParsedDateHistogram dateHistogram = null;
        switch(date) {
            case "month":
                dateHistogram = response.getAggregations().get("by_month");
                break;
            case "week":
                dateHistogram = response.getAggregations().get("by_week");
                break;
            case "day":
                dateHistogram = response.getAggregations().get("by_day");
                break;
            case "hour":
                dateHistogram = response.getAggregations().get("by_hour");
                break;
            case "minutes":
                dateHistogram = response.getAggregations().get("by_minute");
                break;
            default:
                dateHistogram = response.getAggregations().get("by_day");
                break;
        }

        List<ChartTemplate> list = new ArrayList<ChartTemplate>();
        dateHistogram.getBuckets().forEach(histogram ->{
            ChartTemplate histogramTemplate = new ChartTemplate(histogram.getKeyAsString());
            List<ChartTemplate> levels = new ArrayList<>();
            ParsedFilters by_levelBuckets = histogram.getAggregations().get("by_level");
            by_levelBuckets.getBuckets().forEach(level ->{
                ChartTemplate levelTemplate = new ChartTemplate(level.getKeyAsString());
                levelTemplate.setValue(level.getDocCount());
                levels.add(levelTemplate);
            });
            histogramTemplate.setValue(histogram.getDocCount());
            histogramTemplate.setSubData(levels);
            list.add(histogramTemplate);
        });

        return ResultVo.renderOk(list).withRemark("按照日期查询成功！");
    }

    @Override
    public ResultVo countByApp(String namespace) throws IOException {
        SearchSourceBuilder search = new SearchSourceBuilder();
        search.explain(false).size(0).trackTotalHits(true);
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (namespace!=null) boolQuery.should(QueryBuilders.matchQuery("kubernetes.namespace_name", namespace));
        search.query(boolQuery);
        boolQuery.mustNot(QueryBuilders.matchQuery("kubernetes.namespace_name.keyword", "kube-system"));
        boolQuery.mustNot(QueryBuilders.matchQuery("kubernetes.namespace_name.keyword", "default"));
        search.aggregation(AggregationBuilders
                .terms("by_container").field("kubernetes.container_name.keyword").subAggregation(AggregationBuilders.terms("by_instance").field("kubernetes.pod_name.keyword")));
        SearchRequest request = new SearchRequest();
        request.source(search).indices(index);

        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        ParsedStringTerms by_container = response.getAggregations().get("by_container");
        Map<String, Object> containers = new HashMap<String, Object>();
        by_container.getBuckets().forEach(container -> {
            containers.put("total", (long)containers.getOrDefault("total", 0l)+container.getDocCount());
            Map<String, Object> instances = new HashMap<String, Object>();
            ParsedStringTerms by_instance = container.getAggregations().get("by_instance");
            by_instance.getBuckets().forEach(instance ->{
                instances.put("total", (long)instances.getOrDefault("total", 0l)+instance.getDocCount());
                instances.put(instance.getKeyAsString(), instance.getDocCount());
            });
            containers.put(container.getKeyAsString(), instances);
        });

        return ResultVo.renderOk(containers).withRemark("按照应用查询成功");
    }
}

/**
 * 统计数据模板
 * @author User
 *
 */
class ChartTemplate {
    private String name;
    private long value;
    List<ChartTemplate> subData;
    public ChartTemplate() {
    }
    public ChartTemplate(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public long getValue() {
        return value;
    }
    public void setValue(long value) {
        this.value = value;
    }
    public List<ChartTemplate> getSubData() {
        return subData;
    }
    public void setSubData(List<ChartTemplate> subData) {
        this.subData = subData;
    }

}
