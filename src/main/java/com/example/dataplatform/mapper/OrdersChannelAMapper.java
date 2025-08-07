package com.example.dataplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dataplatform.model.Orders;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface OrdersChannelAMapper extends BaseMapper<Orders> {

    /**
     * 自定义的动态插入方法
     */
    @Insert("INSERT INTO ${order.dynamicTableName} (order_id, amount, order_time) VALUES (#{order.orderId}, #{order.amount}, #{order.orderTime})")
    int insertDynamic(@Param("order") Orders order);

    /**
     * 自定义的动态清空方法
     */
    @Delete("DELETE FROM ${dynamicTableName}")
    int deleteAll(@Param("dynamicTableName") String dynamicTableName);

    /**
     * 自定义的动态查询全部数据的方法
     */
    @Select("SELECT * FROM ${dynamicTableName}")
    List<Orders> selectAll(@Param("dynamicTableName") String dynamicTableName);

    /**
     * 自定义的根据备注查询方法
     */
    @Select("SELECT * FROM ${dynamicTableName} WHERE remark = #{remark}")
    List<Orders> selectByRemark(@Param("dynamicTableName") String dynamicTableName, @Param("remark") String remark);

    /**
     * 自定义的根据金额查询方法
     */
    @Select("SELECT * FROM ${dynamicTableName} WHERE amount = #{amount}")
    List<Orders> selectByAmount(@Param("dynamicTableName") String dynamicTableName, @Param("amount") BigDecimal amount);

}