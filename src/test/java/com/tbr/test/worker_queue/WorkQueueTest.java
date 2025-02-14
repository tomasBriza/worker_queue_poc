package com.tbr.test.worker_queue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

public class WorkQueueTest {

    private static final Logger LOGGER = getLogger(WorkQueueTest.class);
    public static final Duration SLEEP_DURATION = Duration.ofMillis(250);

    private final Random random = new Random();
    private final Semaphore semaphore = new Semaphore(9);
    private final Semaphore doneSemaphore = new Semaphore(0);
    private final AtomicInteger created = new AtomicInteger(0);
    private final AtomicInteger processed = new AtomicInteger(0);


    @Test
    public void basicTest() throws Exception {
        LOGGER.info("Running basic test");

        WorkQueue<String> queue = new WorkQueue<>(new WorkQueue.WorkerConfig(3, 10), this::producer, this::consumer);

        queue.start();

        // Start work by 10 workers, should result in creating and processing 10 new items
        doneSemaphore.acquire();

        Assertions.assertEquals(10, created.get());
        Assertions.assertEquals(10, processed.get());

        // Finish work by two workers, should result in creating and processing two new items
        semaphore.release(2);
        doneSemaphore.acquire();

        Assertions.assertEquals(12, created.get());
        Assertions.assertEquals(12, processed.get());

        // Finish work by 5 workers, should result in creating and processing 5 new items
        semaphore.release(5);
        doneSemaphore.acquire();

        Assertions.assertEquals(17, created.get());
        Assertions.assertEquals(17, processed.get());

        // Allow another 20 processes to be created
        semaphore.release(20);
        doneSemaphore.acquire();

        // ensure all work is done
        Assertions.assertEquals(37, created.get());
        Assertions.assertEquals(37, processed.get());

        queue.stop(10, TimeUnit.SECONDS);

    }

    @Test
    public void testEmptyQueue() throws Exception {
        LOGGER.info("Testing empty queue behavior");
        WorkQueue<String> queue = new WorkQueue<>(new WorkQueue.WorkerConfig(1, 1), Optional::empty, this::consumer);
        queue.start();
        Thread.sleep(SLEEP_DURATION); // Let it run for a while to simulate no production
        queue.stop(5, TimeUnit.SECONDS);
        Assertions.assertEquals(0, created.get(), "No items should be created.");
        Assertions.assertEquals(0, processed.get(), "No items should be processed.");
    }

    @Test
    public void testErrorHandlingInConsumer() throws Exception {
        LOGGER.info("Testing error handling in consumer");
        Consumer<String> faultyConsumer = item -> {
            throw new RuntimeException("Simulated consumer error");
        };
        WorkQueue<String> queue = new WorkQueue<>(new WorkQueue.WorkerConfig(1, 1), this::producer, faultyConsumer);
        queue.start();
        Thread.sleep(SLEEP_DURATION); // Allow some time for errors to occur
        queue.stop(5, TimeUnit.SECONDS);
        Assertions.assertTrue(created.get() > 0, "Items should have been created before error occurred.");
    }

    private Optional<String> producer() {
        try {
            // Simulate some work
            Thread.sleep(random.nextInt(250));
            created.incrementAndGet();
        } catch (InterruptedException ignored) {
        }
        return Optional.of(UUID.randomUUID().toString());
    }

    private void consumer(String workItem) {
        LOGGER.info("Processing: {}", workItem);
        try {
            processed.incrementAndGet();
            if (!semaphore.tryAcquire()) {
                LOGGER.info("Semaphore full, waiting");
                doneSemaphore.release(); // Signal that we are done first batch
                semaphore.acquire();
            }
            Thread.sleep(random.nextInt(500));

        } catch (InterruptedException ignored) {
        }
    }
}
