package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //异常情况处理（收货地址为空、购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userId = BaseContext.getCurrentId();

        //查询当前用户的购物车数据
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构造订单数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setAddress(addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));

        //向订单表插入1条数据
        orderMapper.insert(orders);

        //构造订单明细集合
        List<OrderDetail> orderDetailList = new ArrayList<OrderDetail>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        //向订单明细表插入n条数据
        orderDetailMapper.insertBatch(orderDetailList);

        //清空该用户购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //来单提醒（消息提示和语音播报）
        Map map = new HashMap();
        map.put("type", 1);//1为来单提醒，2为客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + outTradeNo);
        webSocketServer.sendToAllClient(JSONObject.toJSONString(map));
    }

    @Override
    public void reminder(Long id) {
        //查询订单是否存在
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map map = new HashMap();
        map.put("type",2);//1表示用户下单 2表示用户催单
        map.put("orderId",id);
        map.put("content","订单号："+orders.getNumber());
        webSocketServer.sendToAllClient(JSONObject.toJSONString(map));
    }

    @Override
    public PageResult historyOrdersQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> ordersList = orderMapper.pageQuery(ordersPageQueryDTO);

        return new PageResult(ordersList.getTotal(),ordersList.getResult());
    }

    @Override
    public OrderVO getOrderDetail(Long id) {
        OrderVO orderVO = new OrderVO();
        Orders orders = orderMapper.getById(id);
        try {
            String jsonStr = objectMapper.writeValueAsString(orders);
            orderVO.setOrderDishes(jsonStr);
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
            orderVO.setOrderDetailList(orderDetailList);
            return orderVO;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void userCancelOrderById(Long id) throws Exception {
        Orders ordersDB = orderMapper.getById(id);

       if(ordersDB == null){
           throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
       }

       if(ordersDB.getStatus() != Orders.PENDING_PAYMENT || ordersDB.getPayStatus() != Orders.TO_BE_CONFIRMED){
           throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
       }

       //使用新的对象只复制需要修改的字段，简洁性能好
       Orders orders = new Orders();
       orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    ordersDB.getNumber(), //商户订单号
                    ordersDB.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

       orders.setStatus(Orders.CANCELLED);
       orders.setOrderTime(LocalDateTime.now());
       orders.setCancelReason("用户取消");
       orderMapper.update(orders);
    }

    @Override
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();

        //根据id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        //把当前订单详情插入回购物车
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x->{
            ShoppingCart shoppingCart = new ShoppingCart();

            BeanUtils.copyProperties(x,shoppingCart);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOs = new ArrayList<>();
        List<Orders> ordersList = page.getResult();
        if(!CollectionUtils.isEmpty(ordersList)){
            for(Orders orders : ordersList){
                OrderVO orderVO = new OrderVO();
                try {
                    String jsonObject = objectMapper.writeValueAsString(orders);
                    orderVO.setOrderDishes(jsonObject);
                    List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
                    orderVO.setOrderDetailList(orderDetailList);
                    orderVOs.add(orderVO);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return new PageResult(page.getTotal(),orderVOs);
    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(orderMapper.countByStatus(Orders.TO_BE_CONFIRMED));
        orderStatisticsVO.setConfirmed(orderMapper.countByStatus(Orders.CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS));

        return orderStatisticsVO;
    }
}
