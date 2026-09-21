package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    /*
    *
    * 插入订单数据
    *
    * */
    void insert(Orders orders);

    /*
    *
    * 根据订单号查询订单
    *
    * */
    @Select("select * from orders where number=#{number}")
    Orders getByNumber(String number);

    /*
    *
    * 更新订单
    *
    * */
    void update(Orders orders);

    /*
    *
    *根据状态和下单时间查询订单
    *
    * */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);
}
