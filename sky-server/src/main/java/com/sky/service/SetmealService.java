package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    /*
    *
    * 新增套餐,保存套餐与菜品关联关系
    *
    * */
    void saveWithDish(SetmealDTO setmealDTO);

    /*
    *
    * 分页查询
    *
    * */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /*
    *
    * 批量删除套餐
    *
    * */
    void deleteBatch(List<Long> ids);

    /*
    *
    * 根据id查询套餐和关联的菜品数据
    *
    * */
    SetmealVO getByIdWithDish(Long id);

    /*
    *
    * 修改套餐
    *
    * */
    void update(SetmealDTO setmealDTO);

    /*
    *
    * 起售、停售套餐
    *
    * */
    void stopOrStart(Integer status, Long id);

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);
}
