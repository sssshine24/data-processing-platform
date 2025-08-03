package com.example.dataplatform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Lombok;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

//Lombok的注解，简化代码
@Data //get、set等常用方法
@NoArgsConstructor //无参构造方法
@AllArgsConstructor //有参构造方法

public class Orders {
    // 这个类将同时用于两张表
    //MyBatis-Plus 的注解
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderId;
    private BigDecimal amount;
    private LocalDateTime orderTime;

    //MyBatis-Plus 的注解
    @TableField(exist = false)
    private String dynamicTableName;
}