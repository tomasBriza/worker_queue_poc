# WorkQueue: A Producer-Consumer Queue Implementation

WorkQueue is a simple implementation of a producer-consumer queue system designed to demonstrate concurrency and virtual threads (introduced in Java 21) in a multi-threaded environment. The queue synchronizes producers and consumers using semaphores and blocking queues.

## Features
- **Producer-Consumer Model:** Manages multiple producers and consumers for processing tasks.
- **Producers and Consumers:** Producers add tasks to the queue only when at least one consumer is free.
  Feature prevents fetching new tasks when consumers are busy by processing existing tasks.
- **Virtual Threads:** Uses Java 21 virtual threads for lightweight concurrency.
- **Logging:** Provides detailed logging for both producers and consumers.
- **Graceful Shutdown:** Supports stopping producers and consumers with a timeout.

## Prerequisites
- Java 21 or later

## How to Build and Run

### Clone the Repository
```bash
git clone https://github.com/yourusername/workqueue-demo.git
cd workqueue-demo
```

### Compile and Run
1. Compile the code:
   ```bash
   ./gradlew build
   ```

2. Run the main class (ensure Java 21 is set as your default Java version):
   ```bash
   java -cp build/libs/workqueue-demo.jar com.tbr.test.worker_queue.Main
   ```

## Usage Example
See the `WorkQueueTest` for an example of how to use the `WorkQueue` class.

## Configuration
The number of producers and consumers can be configured via the `WorkerConfig` record:

## Logging
Logging is handled by SLF4J and can be configured using `logback.xml` or other logging frameworks. By default, the logs are written to the console.

## Potential Use Cases
- Task processing systems.
- Message queues.
- Simulations for concurrent task management.

## Future Improvements
- Configurable queue size.
- Error-handling strategies.
- Metrics collection for performance analysis.

## Contributing
Contributions are welcome! Feel free to open issues or submit pull requests.

## License
This project is licensed under the MIT License. See the `LICENSE` file for more details.

## Contact
For any questions or feedback, please reach out to briza.tomas@gmail.com.

