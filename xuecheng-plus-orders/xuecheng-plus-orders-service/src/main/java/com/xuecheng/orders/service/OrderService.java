package com.xuecheng.orders.service;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcPayRecord;

/**
 * @author Kuroko
 * @description
 * @date 2023/6/5 15:53
 */
public interface OrderService {
    /**
     * 创建商品订单
     * @param userId        用户 id
     * @param addOrderDto   订单信息
     * @return
     */
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);

    /**
     * 查询支付交易记录
     * @param payNo 交易记录号
     * @return
     */
    public XcPayRecord getPayRecordByPayNo(String payNo);

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付记录id
     * @return  支付记录信息
     */
    public PayRecordDto queryPayResult(String payNo);

    /**
     * 保存支付结果信息
     * @param payStatusDto 支付结果信息
     */
    public void saveAlipayStatus(PayStatusDto payStatusDto);

    /**
     * 发送通知结果
     * @param message
     */
    public void notifyPayResult(MqMessage message);


}
