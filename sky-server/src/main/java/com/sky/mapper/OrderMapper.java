package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /*
    *
    * 根据id查询订单
    *
    * */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    /*
    *
    * 根据动态条件统计营业额
    *
    * */
    Double sumByMap(Map map);

    /*
    *
    * 根据动态条件统计订单数量
    *
    * */
    Integer countByMap(Map map);

    /*
    *
    * 根据时间区间统计销量前十名
    *
    * */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);

    /*
    *
    * 根据动态条件分页查询订单
    *
    * */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);
}
