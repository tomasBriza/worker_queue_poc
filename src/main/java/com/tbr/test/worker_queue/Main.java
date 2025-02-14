package com.tbr.test.worker_queue;

import org.slf4j.Logger;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.slf4j.LoggerFactory.getLogger;

public class Main {

    private static final Logger LOGGER = getLogger(Main.class);
    private static final Logger PRODUCER_LOGGER = getLogger(Main.class + ".Producer");
    private static final Logger CONSUMER_LOGGER = getLogger(Main.class + ".Consumer");

    public static void main(String[] args) throws Exception {
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);

        Supplier<Optional<Double>> producer = () -> {
            produced.incrementAndGet();
            var randomTask = Math.random() * 1000;
            PRODUCER_LOGGER.info("Produced item: {}", randomTask);
            return Optional.of(randomTask);
        };
        Consumer<Double> consumer = item -> {
            try {
                Thread.sleep(Duration.ofMillis(250));
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted consumer", e);
            }
            consumed.incrementAndGet();
            CONSUMER_LOGGER.info("Consumed item: {}", item);
        };
        WorkQueue<Double> queue = new WorkQueue<>(new WorkQueue.WorkerConfig(2, 2), producer, consumer);
        queue.start();
        // Stop the queue after 5 seconds
        Thread.sleep(5000);
        queue.stop(5, TimeUnit.SECONDS);

        LOGGER.info("Produced: {}", produced.get());
        LOGGER.info("Consumed: {}", consumed.get());
    }
}
