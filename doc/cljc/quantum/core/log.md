# quantum.core.log

## Some desired inclusions in a log-entry-list header

- Version info, etc.
- PID
- Start time
- Etc.

## Some desired inclusions in a log entry

- Log-entry-specific data
  - Data, if any, as per `ex-info` — might include stack trace
  - Message, if any
  - Perhaps tags
- Log level
  - E.g. Error, Warn, Info, Debug, Trace, Dev, but configurable
- Time(stamp) in nanos according to `System.getNanos` offset by initial `getCurrentTimeMillis`
- Current thread
- Current function
- Etc.

## Encoding

- Text, optionally compressed, optionally pretty-printed
- Binary, optionally compressed
- Etc.

## Routing|Output

- `stdout` (default for levels not going to `stderr`)
- `stderr` (default for `warn` and `error` levels)
- File
- Queue, optionally durable
- Socket (HTTP, WebSocket, etc.)
- In-memory data structure
- Etc.

## Other goals

- Performant defaults
  - Currently, for Clojure, this might be Chronicle Queue
  - For JS, not sure

## Possible goals

- Log rotation, but this is likely orthogonal
  - E.g. rotation of log entries by size and number of entries to keep

## Existing implementations

### [Chronicle Logger](https://github.com/OpenHFT/Chronicle-Logger)

A sub-microsecond Java logger that logs to `Chronicle Queue`.

> Loggers can affect your system performance, therefore logging is sometimes kept to a minimum, With chronicle we aim to eliminate this added overhead, freeing your system to focus on the business logic.
Chronicle logger is built on Chronicle Queue. It provides multiple Chronicle Queue adapters and is a low latency, high throughput synchronous writer. ... Unlike asynchronous writers, you will always see the last message before the application dies. The last message is often the most valuable.

### SLF4J (Simple Logging Façade for Java)

A useful abstraction.

### JCL

[SLF4J is the fix/replacement for JCL.](https://www.slf4j.org/faq.html#yet_another_facade)

### `java.util.logging.*` (JUL)

Reportedly difficult to use.

### Log4J (versions 1 and 2)

### Logback

### Apache Commons Logging

Another façade.
