package com.snapshop.inventory;

import com.snapshop.inventory.dto.InventoryDeductDTO;
import com.snapshop.inventory.dto.InventoryResponseDTO;
import com.snapshop.inventory.entity.StockRecord;
import com.snapshop.inventory.mapper.SkuStockMapper;
import com.snapshop.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存扣减并发测试
 * 验证高并发场景下库存不会出现超卖
 */
@SpringBootTest
class InventoryConcurrentTest {

    /** 测试用商品规格ID */
    private static final Long TEST_SKU_ID = 99999L;

    /** 初始可用库存 */
    private static final int INITIAL_STOCK = 10;

    /** 并发线程数（远超库存，验证不超卖） */
    private static final int THREAD_COUNT = 20;

    /** 每个线程扣减数量 */
    private static final int DEDUCT_QUANTITY = 1;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SkuStockMapper skuStockMapper;

    @BeforeEach
    void setUp() {
        // 初始化测试库存数据：直接更新可用库存为初始值
        StockRecord stock = skuStockMapper.selectById(TEST_SKU_ID);
        if (stock == null) {
            // 创建测试库存记录
            stock = new StockRecord();
            stock.setSkuId(TEST_SKU_ID);
            stock.setAvailableStock(INITIAL_STOCK);
            stock.setLockedStock(0);
            stock.setSoldStock(0);
            stock.setVersion(0);
            skuStockMapper.insert(stock);
        } else {
            // 重置库存为初始值
            stock.setAvailableStock(INITIAL_STOCK);
            stock.setLockedStock(0);
            stock.setSoldStock(0);
            stock.setVersion(0);
            skuStockMapper.updateById(stock);
        }
    }

    @Test
    @DisplayName("并发扣减库存 - 不超卖验证")
    void concurrentDeductShouldNotOversell() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 启动 THREAD_COUNT 个线程同时扣减
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadIndex = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // 等待统一发令枪
                    InventoryDeductDTO dto = new InventoryDeductDTO();
                    dto.setRequestId("CONCURRENT_TEST_" + threadIndex);
                    dto.setBusinessKey("CONCURRENT_TEST_BIZ_" + threadIndex);
                    dto.setSkuId(TEST_SKU_ID);
                    dto.setQuantity(DEDUCT_QUANTITY);
                    dto.setScene("并发测试");

                    InventoryResponseDTO result = inventoryService.deductStock(dto);
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            }).start();
        }

        // 发令枪：所有线程同时启动
        startLatch.countDown();

        // 等待所有线程完成
        finishLatch.await();

        // 验证扣减成功次数不超过初始库存
        int actualSuccess = successCount.get();
        int actualFail = failCount.get();

        System.out.println("===== 并发扣减测试结果 =====");
        System.out.println("初始库存: " + INITIAL_STOCK);
        System.out.println("并发线程数: " + THREAD_COUNT);
        System.out.println("扣减成功次数: " + actualSuccess);
        System.out.println("扣减失败次数: " + actualFail);

        // 核心断言：成功扣减次数不能超过初始库存
        assertTrue(actualSuccess <= INITIAL_STOCK,
                String.format("超卖！成功扣减 %d 次，但初始库存只有 %d", actualSuccess, INITIAL_STOCK));

        // 断言：总线程数 = 成功 + 失败
        assertEquals(THREAD_COUNT, actualSuccess + actualFail,
                String.format("线程总数不匹配: success=%d + fail=%d != %d", actualSuccess, actualFail, THREAD_COUNT));

        // 验证数据库最终库存不为负数
        StockRecord finalStock = skuStockMapper.selectById(TEST_SKU_ID);
        assertNotNull(finalStock, "最终库存记录不应为空");
        System.out.println("最终可用库存: " + finalStock.getAvailableStock());
        System.out.println("版本号: " + finalStock.getVersion());

        assertTrue(finalStock.getAvailableStock() >= 0,
                String.format("库存为负数！availableStock=%d", finalStock.getAvailableStock()));

        // 验证最终库存 = 初始库存 - 成功扣减次数
        int expectedFinalStock = INITIAL_STOCK - actualSuccess;
        assertEquals(expectedFinalStock, finalStock.getAvailableStock(),
                String.format("最终库存与预期不符: expected=%d, actual=%d",
                        expectedFinalStock, finalStock.getAvailableStock()));
    }
}
