package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /*
    *
    * 批量插入订单明细
    *
    * */
    void insertBatch(List<OrderDetail> orderDetailList);

    /*
    *
    * 返回订单相关的明细数据
    *
    * */
    @Select("select * from order_detail where order_id=#{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);
}
