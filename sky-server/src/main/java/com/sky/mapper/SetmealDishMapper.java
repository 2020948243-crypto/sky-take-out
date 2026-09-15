package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /*
    *
    * 获取与菜品关联套餐id
    *
    * */
    List<Long> getSetmealIdsByDishIds(List<Long> ids);

    /*
    *
    * 插入套餐菜品关系
    *
    * */
    @Insert("insert into setmeal_dish (setmeal_id, dish_id, name, price, copies) " +
            "VALUES (#{setmealId},#{dishId},#{name},#{price},#{copies})")
    @AutoFill(OperationType.INSERT)
    void add(SetmealDish setmealDish);

    /*
    *
    * 更新套餐菜品关系
    *
    * */
    void update(SetmealDish setmealDish);

    /*
    *
    * 根据套餐id删除套餐和菜品关联数据
    *
    * */
    @Delete("delete from setmeal_dish where setmeal_id=#{setmealId}")
    void deleteBySetmealId(Long setmealId);

    /*
    *
    * 根据套餐id查询菜品
    *
    * */
    @Select("select * from setmeal_dish where setmeal_id=#{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);
}
