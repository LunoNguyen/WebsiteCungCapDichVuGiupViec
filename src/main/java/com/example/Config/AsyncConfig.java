package com.example.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Bật xử lý bất đồng bộ (@Async) cho các việc chạy nền, ví dụ phát thông báo tới nhiều người nhận.
 * Người dùng bấm "Gửi" là nhận phản hồi ngay; việc ghi cho từng người nhận chạy ở luồng nền.
 *
 * Hàng đợi có giới hạn: khi quá tải, việc mới chạy ngay trên luồng của người gọi (CallerRunsPolicy)
 * thay vì bị bỏ qua, nên không mất việc nào.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("neatify-nen-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
