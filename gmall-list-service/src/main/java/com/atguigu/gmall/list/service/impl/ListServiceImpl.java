package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.ListService;
import com.sun.org.apache.bcel.internal.generic.NEW;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    public static final String ES_INDEX = "gmall";

    public static final String ES_TYPE = "SkuInfo";

    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;

    //将skuLsInfo保存到ES中
    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {

        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        //执行保存动作
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //根据条件查询数据返回结果==全文检索
    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        //1.制作query查询语句
        String query = makeQueryStringForSearch(skuLsParams);
        //2.定义查询动作
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        //3.获取返回结果集
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //4.将结果集转换为SkuLsResult
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams, searchResult);
        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        try {
            Double hotScore = jedis.zincrby("hotScope", 1, skuId);
            if (hotScore % 10 == 0) {
                //更新ES中的hotScope
                updateHotScore(skuId, Math.round(hotScore));
            }
        } catch (Exception e) {
            jedis.close();
        }

    }

    ////更新ES中的hotScope方法
    private void updateHotScore(String skuId, long hotScore) {
        String updQuery = "{\n" +
                "  \"doc\": {\n" +
                "    \"hotScore\":"+hotScore+"\n" +
                "  }\n" +
                "}";
        //执行动作
        Update update = new Update.Builder(updQuery).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 制作返回的结果集
     *
     * @param skuLsParams
     * @param searchResult
     * @return
     */
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();
        //定义初始化集合
        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        //获取到结果集中的SkuLsInfo对象
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        //判断hits不为空
        if (hits != null && hits.size() > 0) {
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo skuLsInfo = hit.source;
                //获取高亮字段skuName
                if (hit.highlight != null && hit.highlight.size() > 0) {
                    List<String> skuNameList = hit.highlight.get("skuName");
                    String skuName = skuNameList.get(0);
                    //将原来的skuName进行覆盖
                    skuLsInfo.setSkuName(skuName);
                }
                skuLsInfoArrayList.add(skuLsInfo);
            }
        }
        //将获取到的skuLsInfo对象设置掉skuLResult中
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);
        //设置total
        skuLsResult.setTotal(skuLsResult.getTotal());
        //设置totalpages
        long totalPage = (searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPage);

        //设置平台属性值
        ArrayList<String> skuAttrValueIdList = new ArrayList<>();
        //获取平台属性值ID
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        if (groupby_attr != null) {
            List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
            for (TermsAggregation.Entry bucket : buckets) {
                skuAttrValueIdList.add(bucket.getKey());
            }
        }
        skuLsResult.setAttrValueIdList(skuAttrValueIdList);

        return skuLsResult;
    }

    /**
     * 制作query语句方法 dsl语句
     *
     * @param skuLsParams
     * @return
     */
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //定义一个查询器 query : {}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //定义查询器下面的bool bool: {}
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断查询条件中是否有keyword 存在则拼接条件 否则跳过
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0) {
            //定义查询 "match": {}
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            //完成bool -- must -- match
            boolQueryBuilder.must(matchQueryBuilder);
            //查询出的信息设置高亮  "highlight": {"pre_tags": [ "<span color='green'>" ], "post_tags": ["</span>"]
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();
            highlighter.preTags("<span style=color:red>");
            highlighter.postTags("</span>");
            highlighter.field("skuName");
            //将设置好的高亮对象 设置到查询器中
            searchSourceBuilder.highlight(highlighter);
        }
        //判断查询条件中是否有catalog3Id 存在则拼接条件 否则跳过
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
            //{"term": {"catalog3Id": "61"}
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }
        //判断查询条件中是否有valueId 存在则拼接条件 否则跳过
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            //"term": {"skuAttrValueList.valueId": "83"}
            for (String valueIds : skuLsParams.getValueId()) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueIds);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        //query
        searchSourceBuilder.query(boolQueryBuilder);

        //设置分页 "from": 0   "size": 4  (pageNo-1)*pageSize
        int from = ((skuLsParams.getPageNo() - 1) * skuLsParams.getPageSize());
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());
        //设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        //设置聚合
        TermsBuilder groupbyAttr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupbyAttr);

        String queryResult = searchSourceBuilder.toString();
        return queryResult;
    }
}
