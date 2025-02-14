package com.tbr.test.worker_queue;

import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * WorkQueueImpl is an implementation of a worker queue system.
 * It manages producers that generate work items and consumers that process those items.
 * This implementation ensures synchronization between producers and consumers using a semaphore
 * and a blocking queue to coordinate work item production and consumption.
 */
public class WorkQueue<T> {

    private static final Logger PRODUCER_LOGGER = getLogger(WorkQueue.class.getName() + ".Producer");
    private static final Logger CONSUMER_LOGGER = getLogger(WorkQueue.class.getName() + ".Consumer");

    private final BlockingQueue<T> workQueue = new LinkedBlockingQueue<>(1);
    private final Semaphore semaphore = new Semaphore(0);

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Supplier<Optional<T>> producer;
    private final Consumer<T> consumer;
    private final WorkerConfig workerConfig;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public WorkQueue(WorkerConfig workerConfig, Supplier<Optional<T>> producer, Consumer<T> consumer) {
        this.producer = producer;
        this.consumer = consumer;
        this.workerConfig = workerConfig;
    }

    public void start() {
        startProducers();
        startConsumers();
    }

    public void stop(long timeout, TimeUnit unit) throws InterruptedException {
        running.set(false);
        PRODUCER_LOGGER.info("Stopping all producers and consumers.");
        if (!executor.awaitTermination(timeout, unit)) {
            executor.shutdownNow();
            PRODUCER_LOGGER.warn("Forced shutdown initiated due to timeout.");
        } else {
            PRODUCER_LOGGER.info("All producers and consumers stopped successfully.");
        }
    }

    private void startConsumers() {
        for (int i = 0; i < workerConfig.countOfWorkers(); i++) {
            consumer(i);
        }
    }

    private void startProducers() {
        for (int i = 0; i < workerConfig.countOfProducers(); i++) {
            producer(i);
        }
    }

    private void consumer(int index) {
        executor.submit(() -> {
            try {
                while (running.get()) {

                    semaphore.release(); // Allow a producer to proceed.
                    CONSUMER_LOGGER.trace("Consumer [{}] released a permit. Available permits: [{}]", index, semaphore.availablePermits());
                    T workItem = workQueue.take(); // Take a work item from the queue.
                    CONSUMER_LOGGER.debug("Consumer [{}] started processing work item: [{}]", index, workItem);
                    try {
                        consumer.accept(workItem); // Process the work item.
                        CONSUMER_LOGGER.trace("Consumer [{}] successfully processed work item: [{}]", index, workItem);
                    } catch (Exception e) {
                        CONSUMER_LOGGER.warn("Consumer [{}] encountered an error while processing work item. Error: {}", index, e.getMessage(), e);
                    }
                }
            } catch (InterruptedException e) {
                PRODUCER_LOGGER.warn("Consumer [{}] has been interrupted and stopped.", index);
                Thread.currentThread().interrupt();
            }
            CONSUMER_LOGGER.info("Consumer [{}] stopped.", index);
        });
    }

    private void producer(int index) {
        executor.submit(() -> {
            try {
                while (running.get()) {
                    PRODUCER_LOGGER.trace("Producer [{}] waiting for a permit. Available permits: [{}]", index, semaphore.availablePermits());
                    semaphore.acquire(); // Wait for a permit to produce.
                    PRODUCER_LOGGER.trace("Producer [{}] acquired a permit. Remaining permits: [{}]", index, semaphore.availablePermits());
                    PRODUCER_LOGGER.trace("Producer [{}] preparing to generate a work item. Current queue size: [{}]", index, workQueue.size());
                    Optional<T> workItem;
                    try {
                        workItem = producer.get(); // Produce a work item.
                        if (workItem.isPresent()) {
                            PRODUCER_LOGGER.debug("Producer [{}] generated a work item: [{}]", index, workItem);
                            workQueue.put(workItem.get()); // Add the work item to the queue.
                            PRODUCER_LOGGER.trace("Producer [{}] successfully added work item to the queue.", index);
                        } else {
                            PRODUCER_LOGGER.trace("Producer [{}] did not generate a work item.", index);
                        }
                    } catch (Exception e) {
                        PRODUCER_LOGGER.warn("Producer [{}] encountered an error while generating a work item. Error: {}", index, e.getMessage(), e);
                    }
                }
            } catch (InterruptedException e) {
                PRODUCER_LOGGER.warn("Producer [{}] has been interrupted and stopped.", index);
                Thread.currentThread().interrupt();
            }
            PRODUCER_LOGGER.info("Producer [{}] stopped.", index);
        });
    }

    public record WorkerConfig(int countOfProducers, int countOfWorkers) {
    }
}
