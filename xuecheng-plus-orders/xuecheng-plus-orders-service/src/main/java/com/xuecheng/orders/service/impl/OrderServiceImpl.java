package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Kuroko
 * @description
 * @date 2023/6/5 15:59
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    //@Value("${pay.alipay.APP_ID}")
    String APP_ID;

    //@Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    //@Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;
    @Autowired
    XcOrdersMapper xcOrdersMapper;
    @Autowired
    XcPayRecordMapper xcPayRecordMapper;
    @Autowired
    XcOrdersGoodsMapper xcOrdersGoodsMapper;
    @Autowired
    MqMessageService mqMessageService;
    @Autowired
    RabbitTemplate rabbitTemplate;
    //@Value("${pay.qrcodeurl}")    //在nacos中orders-service-dev.yaml配置二维码的url
    String qrcodeurl;


    @Override
    @Transactional
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        // 1. 添加商品订单
        XcOrders xcOrders = saveOrders(userId, addOrderDto);
        // 2. 添加支付交易记录
        XcPayRecord payRecord = createPayRecord(xcOrders);
        // 3. 生成二维码
        String qrcode = null;
        try{
            // 3.1 用订单号填充占位符
            qrcodeurl = String.format(qrcodeurl, payRecord.getPayNo());
            // 3.2 生成二维码
            qrcode = new QRCodeUtil().createQRCode(qrcodeurl, 200, 200);
        }catch (IOException e){
            XueChengPlusException.cast("生成二维码出错!");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        // 补上二维码信息
        payRecordDto.setQrcode(qrcode);
        return payRecordDto;
    }

    /**
     * 查询支付交易记录
     * @param payNo 交易记录号
     * @return
     */
    @Override
    public XcPayRecord getPayRecordByPayNo(String payNo) {
        return xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
    }

    /**
     * 保存订单信息，保存两张表，一张是订单表，一张是订单明细表，一个订单可以有多个商品，一个订单明细表对应一个商品，需要做幂等性判断
     * @param userId        用户 id
     * @param addOrderDto   订单信息
     * @return
     */
    @Transactional
    public XcOrders saveOrders(String userId, AddOrderDto addOrderDto){
        // 1. 幂等性判断
        XcOrders orders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if(orders != null){
            // 已经保存过该订单
            return orders;
        }
        // 2. 插入订单表
        orders = new XcOrders();
        BeanUtils.copyProperties(addOrderDto, orders);
        orders.setId(IdWorkerUtils.getInstance().nextId());
        orders.setCreateDate(LocalDateTime.now());
        orders.setUserId(userId);
        orders.setStatus("600001");
        int insert = xcOrdersMapper.insert(orders);
        if(insert <= 0){
            XueChengPlusException.cast("插入订单记录失败!");
        }
        // 3. 插入订单明细表，订单中的每个商品对应一一条订单明细记录
        Long ordersId = orders.getId();
        String orderDetail = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(orderDetail, XcOrdersGoods.class);
        xcOrdersGoods.forEach(goods -> {
            goods.setOrderId(ordersId);
            int insert1 = xcOrdersGoodsMapper.insert(goods);
            if(insert1 <= 0){
                XueChengPlusException.cast("插入订单明细表失败");
            }
        });

        return orders;
    }

    /**
     * 根据业务 id 查询订单
     * @param businessId
     * @return
     */
    public XcOrders getOrderByBusinessId(String businessId){
        XcOrders xcOrders = xcOrdersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return xcOrders;
    }

    /**
     * 创建支付交易记录
     * @param orders    订单信息
     * @return
     */
    public XcPayRecord createPayRecord(XcOrders orders){
        if(orders == null){
            XueChengPlusException.cast("订单不存在!");
        }
        if("600002".equals(orders.getStatus())){
            XueChengPlusException.cast("订单已支付!");
        }
        // 创建信心的交易记录
        XcPayRecord xcPayRecord = new PayRecordDto();
        // 生成支付交易流水号
        long payNo = IdWorkerUtils.getInstance().nextId();
        xcPayRecord.setPayNo(payNo);
        // 关联商品订单
        xcPayRecord.setOrderId(orders.getId());
        xcPayRecord.setOrderName(orders.getOrderName());
        xcPayRecord.setTotalPrice(orders.getTotalPrice());
        xcPayRecord.setCurrency("CNY");
        xcPayRecord.setCreateDate(LocalDateTime.now());
        xcPayRecord.setStatus("600001");    // 未支付
        xcPayRecord.setUserId(orders.getUserId());

        int insert = xcPayRecordMapper.insert(xcPayRecord);
        if(insert <= 0){
            XueChengPlusException.cast("创建支付交易记录失败!");
        }

        return xcPayRecord;
    }

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付记录id
     * @return
     */
    @Override
    public PayRecordDto queryPayResult(String payNo){
        XcPayRecord payRecord = getPayRecordByPayNo(payNo);
        if(payRecord == null){
            XueChengPlusException.cast("请重新点击获取二维码!");
        }
        // 支付状态
        String status = payRecord.getStatus();
        // 如果支付成功直接返回
        if ("601002".equals(status)) {
            PayRecordDto payRecordDto = new PayRecordDto();
            BeanUtils.copyProperties(payRecord, payRecordDto);
            return payRecordDto;
        }

        // 调用支付宝接口查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        // 拿到支付结果，更新支付记录表和订单表的状态为已支付
        saveAlipayStatus(payStatusDto);
        //重新查询支付记录
        payRecord = getPayRecordByPayNo(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        return payRecordDto;

    }

    /**
     * 调用支付宝接口查询支付结果
     * @param payNo 支付记录id
     * @return 支付记录信息
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo) {
        // 1. 获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        AlipayTradeQueryResponse response = null;
        // 2. 请求查询
        try{
            response = alipayClient.execute(request);
        }catch (AlipayApiException e){
            XueChengPlusException.cast("请求支付宝查询支付结果异常!");
        }
        // 3. 查询失败
        if(!response.isSuccess()){
            XueChengPlusException.cast("请求支付宝查询支付结果异常!");
        }
        // 4. 查询成功,获取结果集
        String body = response.getBody();
        // 4.1 转 map
        Map resultMap = JSON.parseObject(body, Map.class);
        // 4.2 获取需要的信息
        Map<String, String> alipay_trade_query_response = (Map<String, String>) resultMap.get("alipay_trade_query_response");
        // 5. 创建返回对象
        PayStatusDto payStatusDto = new PayStatusDto();
        // 6. 封装返回
        String tradeStatus = alipay_trade_query_response.get("trade_status");
        String outTradeNo = alipay_trade_query_response.get("out_trade_no");
        String tradeNo = alipay_trade_query_response.get("trade_no");
        String totalAmount = alipay_trade_query_response.get("total_amount");
        payStatusDto.setTrade_status(tradeStatus);
        payStatusDto.setOut_trade_no(outTradeNo);
        payStatusDto.setTrade_no(tradeNo);
        payStatusDto.setTotal_amount(totalAmount);
        payStatusDto.setApp_id(APP_ID);
        return payStatusDto;
    }

    /**
     * 保存支付结果信息
     * @param payStatusDto 支付结果信息
     */
    @Override
    @Transactional
    public void saveAlipayStatus(PayStatusDto payStatusDto) {
        // 1. 获取支付交易流水号
        String payNo = payStatusDto.getOut_trade_no();
        // 2. 查询数据库订单状态
        XcPayRecord payRecord = getPayRecordByPayNo(payNo);
        if(payRecord == null){
            XueChengPlusException.cast("未找到支付记录!");
        }
        XcOrders orders = xcOrdersMapper.selectById(payRecord.getOrderId());
        if(orders == null){
            XueChengPlusException.cast("找不到相关订单信息!");
        }
        String status = payRecord.getStatus();
        // 2.1 已支付，直接返回
        if("600002".equals(status)){
            return;
        }
        // 3. 查询支付宝交易状态
        String tradeStatus = payStatusDto.getTrade_status();
        // 3.1 支付宝交易已成功，保存订单表和交易记录表，更新交易状态
        if("TRADE_SUCCESS".equals(tradeStatus)){
            // 更新支付交易表
            payRecord.setStatus("601002");
            payRecord.setOutPayNo(payStatusDto.getTrade_no());
            payRecord.setOutPayChannel("Alipay");
            payRecord.setPaySuccessTime(LocalDateTime.now());
            int updateRecord = xcPayRecordMapper.updateById(payRecord);
            if(updateRecord <= 0){
                XueChengPlusException.cast("更新支付交易表失败!");
            }
            // 更新订单表
            orders.setStatus("600002");
            int updateOrder = xcOrdersMapper.updateById(orders);
            if(updateOrder <= 0){
                XueChengPlusException.cast("更新订单表失败!");
            }

            // 将消息写道数据库
            MqMessage mqMessage = mqMessageService.addMessage("payresult_notify", orders.getOutBusinessId(), orders.getOrderType(), null);
            // 发送消息
            notifyPayResult(mqMessage);
        }
    }

    /**
     * 发送通知结果
     * @param message
     */
    @Override
    public void notifyPayResult(MqMessage message) {
        // 1. 消息体，转 json
        String msg = JSON.toJSONString(message);
        // 设置消息持久化
        Message msgObj = MessageBuilder.withBody(msg.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        // 2. 全局唯一的消息 ID， 封装到 CorrelationData 中
        CorrelationData correlationData = new CorrelationData(message.getId().toString());
        // 3. 添加到 callback
        correlationData.getFuture().addCallback(
                result -> {
                    if(result.isAck()){
                        // 3.1 ack,消息成功
                        log.debug("通知支付结果消息发送成功, ID:{}", correlationData.getId());
                        // 删除消息表中的记录
                        mqMessageService.completed(message.getId());
                    }else{
                        // 3.2 no ack,消息失败
                        log.error("通知支付结果消息发送失败, ID:{}, 原因:{}", correlationData.getId(), result.getReason());
                    }
                },
                ex -> log.error("消息发送异常, ID:{}, 原因:{}", correlationData.getId(), ex.getMessage())
        );

        // 发送消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT, "", msgObj, correlationData);
    }

}
