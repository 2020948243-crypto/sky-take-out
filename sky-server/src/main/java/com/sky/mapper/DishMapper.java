package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /*
    *
    * 插入菜品
    *
    * */
    @AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);

    /*
    *
    * 分页查询菜品
    *
    * */
    Page<DishVO> page(DishPageQueryDTO dishPageQueryDTO);

    /*
    *
    * 通过id查询菜品
    *
    * */
    @Select("select * from dish where id=#{id}")
    Dish getById(Long id);

    /*
    *
    * 通过ids批量删除菜品
    *
    * */
    void deleteByIds(List<Long> ids);

    /*
    *
    * 修改菜品
    *
    * */
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    /*
    *
    * 动态条件查询菜品
    *
    * */
    List<Dish> list(Dish dish);

    /*
    *
    * 根据套餐id查询菜品
    *
    * */
    @Select("select a.* from dish a left join setmeal_dish b on a.id=b.dish_id where b.setmeal_id=#{setmealId}")
    List<Dish> getBySetmealId(Long id);
}
