package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private PaymentService paymentService;

    //下订单
    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        orderInfo.setCreateTime(new Date());
        //设置过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());
        // 生成第三方支付编号
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfoMapper.insertSelective(orderInfo);
        //插入订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList != null && orderDetailList.size() > 0) {
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insertSelective(orderDetail);
            }
        }

        // 返回订单编号
        String orderId = orderInfo.getId();
        return orderId;
    }

    //生成流水号
    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = "user:" + userId + ":tradeCode";
        String tradeCode = UUID.randomUUID().toString();
        jedis.setex(tradeNoKey, 10 * 60, tradeCode);
        jedis.close();
        return tradeCode;
    }

    //比较流水号
    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = "user:" + userId + ":tradeCode";
        String tradeCode = jedis.get(tradeNoKey);
        jedis.close();
        if (tradeCode != null && tradeCode.equals(tradeCodeNo)) {
            return true;
        } else {
            return false;
        }

    }

    //删除流水号
    @Override
    public void delTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = "user:" + userId + ":tradeCode";
        jedis.del(tradeNoKey);
        jedis.close();
    }

    //判断当前是否有库存
    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    //根据订单ID查询订单编号
    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    //清空购物车
    @Override
    public void clean(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = "user:" + userId + ":checked";
        jedis.del(userCartKey);
        jedis.close();
    }

    //消费者接受消息，更新订单状态
    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);

    }

    //根据订单ID发送消息给库存
    @Override
    public void sendOrderStatus(String orderId) {
        //获取连接
        Connection connection = activeMQUtil.getConnection();
        String orderJson = initWareOrder(orderId);
        try {
            //启动
            connection.start();
            //创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建对象
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            //创建消息提供者
            MessageProducer producer = session.createProducer(order_result_queue);
            //创建消息
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            activeMQTextMessage.setText(orderJson);
            producer.send(activeMQTextMessage);

            session.commit();
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    //获取过期订单列表
    @Override
    public List<OrderInfo> getExpiredOrderList() {
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andLessThan("expireTime", new Date()).andEqualTo("processStatus", ProcessStatus.UNPAID);
        return orderInfoMapper.selectByExample(example);
    }

    //处理过期订单
    @Override
    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        // 修改订单信息
        updateOrderStatus(orderInfo.getId(), ProcessStatus.CLOSED);
        // 修改付款信息
        paymentService.closePayment(orderInfo.getId());
    }

    //制作JSON字符串
    private String initWareOrder(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        //制作一个map
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);


    }

    //制作map
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", "丫头");
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());

        //构建details
        ArrayList<Map> detailList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList != null && orderDetailList.size() > 0) {
            for (OrderDetail orderDetail : orderDetailList) {
                HashMap<String, Object> detailMap = new HashMap<>();
                detailMap.put("skuId", orderDetail.getSkuId());
                detailMap.put("skuName", orderDetail.getSkuName());
                detailMap.put("skuNum", orderDetail.getSkuNum());
                detailList.add(detailMap);
            }
        }
        map.put("details", detailList);
        return map;
    }

    //拆单
    @Override
    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        // 1 先查询原始订单
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);

        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        for (Map map : maps) {
            String wareId = (String) map.get("wareId");
            List<String> skuIds = (List<String>) map.get("skuIds");
            //创建新的子订单
            OrderInfo subOrderInfo = new OrderInfo();
            BeanUtils.copyProperties(orderInfoOrigin, subOrderInfo);
            subOrderInfo.setId(null);
            //设置父ID
            subOrderInfo.setParentOrderId(orderId);
            //设置仓库ID
            subOrderInfo.setWareId(wareId);

            //创建一个子订单的集合
            List<OrderDetail> subOrderDetailList = new ArrayList<>();
            //总金额
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            if (orderDetailList != null && orderDetailList.size() > 0) {
                for (OrderDetail orderDetail : orderDetailList) {
                    //循环找出每个库存的商品ID
                    for (String skuId : skuIds) {
                        if (skuId.equals(orderDetail.getSkuId())) {
                            //表示仓库中有商品ID
                            orderDetail.setId(null);
                            subOrderDetailList.add(orderDetail);
                        }
                    }
                }
            }
            //将子订单商品明细集合，添加到子订单中
            subOrderInfo.setOrderDetailList(subOrderDetailList);
            //计算总金额
            subOrderInfo.sumTotalAmount();
            saveOrder(subOrderInfo);
            subOrderInfoList.add(subOrderInfo);
        }
        //修改订单状态
        updateOrderStatus(orderId, ProcessStatus.SPLIT);
        return subOrderInfoList;
    }
}