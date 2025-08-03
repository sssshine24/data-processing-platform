package com.example.dataplatform.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class RedisLockUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取锁
     * @param lockKey 锁的键
     * @param requestId 请求ID，通常是UUID，用于标识谁加的锁
     * @param expireTime 锁的过期时间
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockKey, String requestId, long expireTime) {
        // 使用 setIfAbsent 方法，它对应 Redis 的 SETNX 命令，是原子操作
        // 它的作用是：如果键不存在，才设置键的值，返回 true；如果键存在，不做任何操作，返回 false
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, requestId, expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放锁
     * @param lockKey 锁的键
     * @param requestId 请求ID，确保只有加锁的客户端才能解锁
     */
    public void unlock(String lockKey, String requestId) {
        // 释放锁前，先判断这把锁是不是自己加的
        String storedRequestId = stringRedisTemplate.opsForValue().get(lockKey);
        if (requestId.equals(storedRequestId)) {
            stringRedisTemplate.delete(lockKey);
        }
    }
}