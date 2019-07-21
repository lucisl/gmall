package com.atguigu.gmall.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * redis 的工具类
 */
public class RedisUtil {
    // 创建一个连接池
    private JedisPool jedisPool = null;

    // 创建一个初始化方法 jedisPool = new JedisPool();
    public void initJedisPool(String host, int port, int database) {

        // 需要配置一个连接池的参数对象
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 设置参数 最大连接数
        jedisPoolConfig.setMaxTotal(100);
        // 设置等待时间
        jedisPoolConfig.setMaxWaitMillis(10 * 1000);
        // 设置最大，
        // jedisPoolConfig.setMaxIdle(30);
        // 设置最小剩余数
        jedisPoolConfig.setMinIdle(10);
        // 设置开机自检，检查获取当前的连接是否可以使用！******
        jedisPoolConfig.setTestOnBorrow(true);
        // 如果达到最大连接池的时候，需要等待
        jedisPoolConfig.setBlockWhenExhausted(true);

        jedisPool = new JedisPool(jedisPoolConfig, host, port, 20 * 1000);
    }

    // 获取Jedis
    public Jedis getJedis() {
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }


}
