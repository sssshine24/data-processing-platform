
package com.example.dataplatform.jobhandler;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dataplatform.mapper.OrdersChannelAMapper;
import com.example.dataplatform.model.Orders;
import com.example.dataplatform.util.RedisLockUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class SampleJob {

    @Resource
    private OrdersChannelAMapper ordersChannelAMapper;

    @Resource
    private RedisLockUtil redisLockUtil;

    @XxlJob("helloJobHandler")
    public void helloJobHandler() {
        XxlJobHelper.log("XXL-JOB, Hello from our Data Platform!");
        System.out.println("XXL-JOB, Hello from our Data Platform!");
    }

    /**
     * 任务：模拟拉取每日订单数据任务
     * <p>
     *     1. 清理历史测试数据，确保每次运行环境独立.
     *     2. 为渠道A生成2条订单，为渠道B生成3条订单，以模拟数据差异.
     *     3. 利用自定义的 `insertDynamic` Mapper方法，将订单数据分别持久化到 `orders_channel_a` 和 `orders_channel_b` 表中.
     * </p>
     */
    @XxlJob("fetchOrdersJob")
    public void fetchOrdersJob() {
        XxlJobHelper.log("开始执行【模拟拉取每日订单】任务...");

        // 模拟生成渠道A的两条订单
        for (int i = 0; i < 2; i++) {
            Orders orderA = new Orders(
                    null,
                    UUID.randomUUID().toString().substring(0, 10),
                    new BigDecimal((Math.random() * 100)).setScale(2, BigDecimal.ROUND_HALF_UP),
                    LocalDateTime.now().minusDays(1),
                    "orders_channel_a"  // 传入要操作的表名
            );
            ordersChannelAMapper.insertDynamic(orderA);
            XxlJobHelper.log("向渠道A插入订单：{}", orderA.getOrderId());
        }

        // 模拟生成渠道B的三条订单
        for (int i = 0; i < 3; i++) {
            Orders orderB = new Orders(
                    null,
                    UUID.randomUUID().toString().substring(0, 10),
                    new BigDecimal((Math.random() * 100)).setScale(2, BigDecimal.ROUND_HALF_UP),
                    LocalDateTime.now().minusDays(1),
                    "orders_channel_b"  // 传入要操作的表名
            );
            ordersChannelAMapper.insertDynamic(orderB);
            XxlJobHelper.log("向渠道B插入订单：{}", orderB.getOrderId());
        }

        XxlJobHelper.log("【模拟拉取每日订单】任务执行结束。");
    }

    /**
     * 任务：每日订单对账
     * <p>
     *     1. 利用Redis分布式锁确保任务的幂等性.
     *     2. 清理历史数据，确保每次运行环境独立.
     *     3. 制造用于对账的测试数据，包括“金额不一致”、“单边订单”等场景.
     *     4. 分别查询渠道A和渠道B的订单数据.
     *     5. 对比两个渠道的订单数据，记录差异详情.
     *     6. 输出对账结果，包括成功订单数、失败订单数、差异详情等.
     * </p>
     * * @throws InterruptedException 线程被中断时抛出
     */
    @XxlJob("reconciliationJob")
    public void reconciliationJob() throws InterruptedException {
        String lockKey = "reconciliationJobLock";
        String requestId = UUID.randomUUID().toString();
        boolean lockAcquired = redisLockUtil.tryLock(lockKey, requestId, 600);

        if (lockAcquired) {
            XxlJobHelper.log("成功获取分布式锁，开始执行【每日订单对账】任务...");
            try {
                XxlJobHelper.log("开始清空历史数据...");
                ordersChannelAMapper.deleteAll("orders_channel_a");
                ordersChannelAMapper.deleteAll("orders_channel_b");
                XxlJobHelper.log("历史数据已清空...");

                XxlJobHelper.log("正在生成新的测试数据...");
                Orders order1_a = new Orders(null, "ORDER_001", new BigDecimal("100.00"), LocalDateTime.now().minusDays(1), "orders_channel_a");
                Orders order1_b = new Orders(null, "ORDER_001", new BigDecimal("100.01"), LocalDateTime.now().minusDays(1), "orders_channel_b");
                Orders order2_a = new Orders(null, "ORDER_002", new BigDecimal("200.00"), LocalDateTime.now().minusDays(1), "orders_channel_a");
                Orders order3_b = new Orders(null, "ORDER_003", new BigDecimal("300.00"), LocalDateTime.now().minusDays(1), "orders_channel_b");
                Orders order4_a = new Orders(null, "ORDER_004", new BigDecimal("400.00"), LocalDateTime.now().minusDays(1), "orders_channel_a");
                Orders order4_b = new Orders(null, "ORDER_004", new BigDecimal("400.00"), LocalDateTime.now().minusDays(1), "orders_channel_b");

                ordersChannelAMapper.insertDynamic(order1_a);
                ordersChannelAMapper.insertDynamic(order2_a);
                ordersChannelAMapper.insertDynamic(order4_a);
                ordersChannelAMapper.insertDynamic(order1_b);
                ordersChannelAMapper.insertDynamic(order3_b);
                ordersChannelAMapper.insertDynamic(order4_b);
                XxlJobHelper.log("新的测试数据已生成...");
                TimeUnit.SECONDS.sleep(1);

                XxlJobHelper.log("----------------- 对账开始 -----------------");
                List<Orders> ordersA = ordersChannelAMapper.selectAll("orders_channel_a");
                List<Orders> ordersB = ordersChannelAMapper.selectAll("orders_channel_b");
                Map<String, Orders> mapB = ordersB.stream().collect(Collectors.toMap(Orders::getOrderId, order -> order));
                for (Orders orderA : ordersA) {
                    String orderIdA = orderA.getOrderId();
                    Orders orderB = mapB.get(orderIdA);
                    if (orderB == null) {
                        XxlJobHelper.log("[差异] 渠道A订单 [{}] 在渠道B中不存在！", orderIdA);
                    } else {
                        if (orderA.getAmount().compareTo(orderB.getAmount()) != 0) {
                            XxlJobHelper.log("[差异] 订单 [{}] 金额不一致！渠道A: {}, 渠道B: {}", orderIdA, orderA.getAmount(), orderB.getAmount());
                        }
                        mapB.remove(orderIdA);
                    }
                }
                if (!mapB.isEmpty()) {
                    for (String orderIdB : mapB.keySet()) {
                        XxlJobHelper.log("[差异] 渠道B订单 [{}] 在渠道A中不存在！", orderIdB);
                    }
                }
                XxlJobHelper.log("----------------- 对账结束 -----------------");

            } finally {
                redisLockUtil.unlock(lockKey, requestId);
                XxlJobHelper.log("任务执行完毕，释放分布式锁。");
            }
        } else {
            XxlJobHelper.log("获取分布式锁失败，任务可能正在由另一个实例执行，本次调度跳过。");
        }
    }
    /**
     * 任务：模拟生成对账后的报表
     * <p>
     *     1. 等待对账任务完成，确保数据已准备好.
     *     2. 基于对账结果，生成财务报表.
     *     3. 模拟将报表发送至相关人员邮箱.
     * </p>
     */
    @XxlJob("generateReportJob")
    public void generateReportJob() {
        XxlJobHelper.log("接收到上游任务（对账任务）的指令...");
        XxlJobHelper.log("开始基于对账结果，生成财务报表...");

        // 模拟报表生成过程
        try {
            TimeUnit.SECONDS.sleep(3); // 模拟耗时3秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        XxlJobHelper.log("财务报表已生成！已发送至相关人员邮箱。");
    }
    /**
     * 任务：模拟分片处理海量数据
     * <p>
     *     1. 基于分片参数，从数据库捞取海量订单ID.
     *     2. 每个分片只处理自己负责的那部分数据.
     *     3. 模拟处理耗时.
     * </p>
     */
    @XxlJob("shardingReconciliationJob")
    public void shardingReconciliationJob() {
        // 获取分片参数
        int shardingIndex = XxlJobHelper.getShardIndex(); // 当前分片序号，从0开始
        int shardingTotal = XxlJobHelper.getShardTotal(); // 总分片数

        XxlJobHelper.log("开始执行【分片对账任务】，当前分片: {} / {}", shardingIndex, shardingTotal);

        // 模拟从数据库捞取海量订单ID
        List<Integer> allOrderIds = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            allOrderIds.add(i);
        }

        // 每个分片只处理自己负责的那部分数据
        for (Integer orderId : allOrderIds) {
            if (orderId % shardingTotal == shardingIndex) {
                XxlJobHelper.log("正在处理订单ID: {}, 本任务由分片 {} 负责", orderId, shardingIndex);
                // 模拟处理耗时
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        XxlJobHelper.log("【分片对账任务】分片 {} 执行结束", shardingIndex);
    }

    private static int failCount = 0; // 用一个静态变量来记录失败次数
    /**
     * 任务：模拟失败与重试
     * <p>
     *     1. 模拟一个数据库连接异常，导致任务执行失败.
     *     2. 失败后，重试3次.
     *     3. 每次重试间隔1秒.
     * </p>
     */
    @XxlJob("failJob")
    public void failJob() throws Exception {
        XxlJobHelper.log("开始执行【失败重试与告警测试】任务...");

        failCount++;

        if (failCount < 3) {
            XxlJobHelper.log("第 {} 次执行，模拟执行失败...", failCount);
            throw new RuntimeException("模拟数据库连接异常");
        }

        XxlJobHelper.log("第 {} 次执行，任务成功！", failCount);
        failCount = 0; // 成功后重置计数器
    }

    /**
     * 任务：生成海量模拟数据
     * <p>
     *     1. 生成100万条订单数据.
     *     2. 每次批量插入1000条.
     *     3. 模拟耗时.
     * </p>
     */
    @XxlJob("generateMockDataJob")
    public void generateMockDataJob() {
        XxlJobHelper.log("开始执行【生成海量模拟数据】任务...");
        int totalRecords = 1000000; // 生成100万条订单数据
        int batchSize = 1000; // 每次批量插入1000条

        List<Orders> batchList = new ArrayList<>();

        for (int i = 1; i <= totalRecords; i++) {
            // 生成不重复的订单号
            String orderId = UUID.randomUUID().toString();
            Orders order = new Orders(
                    null,
                    orderId,
                    new BigDecimal((Math.random() * 1000)).setScale(2, BigDecimal.ROUND_HALF_UP),
                    LocalDateTime.now(),
                    "orders_channel_a");
            batchList.add(order);

            if (batchList.size() >= batchSize) {
                // 当累积到一批时，执行插入
                for (Orders o : batchList) {
                    ordersChannelAMapper.insertDynamic(o);
                }
                batchList.clear();
                XxlJobHelper.log("已成功插入 {} / {} 条数据...", i, totalRecords);
            }
        }

        // 处理最后一批不足batchSize的数据
        if (!batchList.isEmpty()) {
            for (Orders o : batchList) {
                ordersChannelAMapper.insertDynamic(o);
            }
            XxlJobHelper.log("已成功插入 {} / {} 条数据...", totalRecords, totalRecords);
        }

        XxlJobHelper.log("【生成海量模拟数据】任务执行结束。");
    }
    /**
     * 任务：模拟一个没有索引的慢查询
     * <p>
     *     1. 基于100万条订单数据，查询一个随机金额的订单.
     *     2. 模拟耗时.
     * </p>
     */
    @XxlJob("slowQueryJob")
    public void slowQueryJob() {
        XxlJobHelper.log("开始执行【无法缓存的慢查询测试】任务...");
        long startTime = System.currentTimeMillis();

        BigDecimal randomAmount = new BigDecimal(Math.random() * -1000); // 生成一个随机的负数金额
        XxlJobHelper.log("将在一百万数据中，查询一个随机金额 {} 的订单...", randomAmount);

        // 执行查询
        List<Orders> result = ordersChannelAMapper.selectByAmount("orders_channel_a", randomAmount);

        long endTime = System.currentTimeMillis();
        XxlJobHelper.log("查询完毕！共找到 {} 条记录，耗时：{} 毫秒", result.size(), (endTime - startTime));
    }

    /**
     * 任务：为备注字段填充数据
     * <p>
     *     1. 模拟基于100万条订单数据，为备注字段填充随机字符串.
     * </p>
     */
    @XxlJob("populateRemarkJob")
    public void populateRemarkJob() {
        XxlJobHelper.log("开始执行【填充备注字段】任务...");
        // 假设它已填充……
        XxlJobHelper.log("正在为一百万条数据填充随机备注... ");

        XxlJobHelper.log("【填充备注字段】任务完成。");
    }

    /**
     * 任务：模拟一个基于字符串的慢查询
     * <p>
     *     1. 基于100万条订单数据，查询一个随机备注的订单.
     *     2. 模拟耗时.
     * </p>
     */
    @XxlJob("slowVarcharQueryJob")
    public void slowVarcharQueryJob() {
        XxlJobHelper.log("开始执行【字符串慢查询测试】任务...");
        long startTime = System.currentTimeMillis();

        // 查询一个随机备注
        String targetRemark = UUID.randomUUID().toString();
        XxlJobHelper.log("将在一百万数据中，查询备注为 {} 的订单...", targetRemark);

        // 使用新的Mapper方法来按备注查询
        List<Orders> result = ordersChannelAMapper.selectByRemark("orders_channel_a", targetRemark);

        long endTime = System.currentTimeMillis();
        XxlJobHelper.log("查询完毕！共找到 {} 条记录，耗时：{} 毫秒", result.size(), (endTime - startTime));
    }
}