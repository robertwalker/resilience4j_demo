# Resilience4J Demo

## Preflight

### Launch the demo application

Launch the demo application with the IDE of your choice.

### Demo Steps

#### Step 1: Send one request to the root path

```zsh
$ curl 'http://localhost:8080/' -i -X GET
```

Response:

```json
{"message":"Exception handled by catch all fallback"}
```

#### Step 2: Start WireMock Standalone

```zsh
java -jar wiremock-jre8-standalone-2.35.0.jar --port 8888
```

#### Step 3: Configure WireMock to return success with no delay

```zsh
# No delay
curl -X POST \
  --data '{ "request": { "url": "/slow/service", "method": "GET" }, "response": { "status": 200, "body": "Here it is!\n" }}' \
  http://localhost:8888/__admin/mappings/new
```

#### Step 4: Send one request to the root path

```zsh
$ curl 'http://localhost:8080/' -i -X GET
```

Response:

```json
{"message":"Here it is!\n"}
```

#### Step 5: Stop and restart WireMock Standalone

From the terminal running WireMock press `^c`.

```zsh
java -jar wiremock-jre8-standalone-2.35.0.jar --port 8888
```

#### Step 6: Configure WireMock to return a fault response

```zsh
# Fault response
curl -X POST \
  --data '{ "request": { "method": "GET", "url": "/slow/service" }, "response": {  "fault": "MALFORMED_RESPONSE_CHUNK" } }' \
  http://localhost:8888/__admin/mappings/new
```

#### Step 7: Send one request to the root path

```zsh
$ curl 'http://localhost:8080/' -i -X GET
```

Response:

```json
{"message":"Exception handled by retry fallback"}
```

#### Step 8: Stop and restart WireMock Standalone

From the terminal running WireMock press `^c`.

```zsh
java -jar wiremock-jre8-standalone-2.35.0.jar --port 8888
```

#### Step 9: Configure WireMock to return success with a 3-second delay

```zsh
# 3 second delay
curl -X POST \
  --data '{ "request": { "method": "GET", "url": "/slow/service" }, "response": { "status": 200, "body": "Here it is!\n", "fixedDelayMilliseconds": 3000 } }' \
  http://localhost:8888/__admin/mappings/new
```

#### Step 10: Send one request to the root path

```zsh
$ curl 'http://localhost:8080/' -i -X GET
```

Response:

```json
{"message":"Exception handled by time limiter fallback"}
```

### CircuitBreaker Demo

#### Step 1: Stop and restart `DemoApplication`

Stop the DemoApplication and restart it in order to clear the recorded metrics.

#### Step 2: Stop and restart WireMock Standalone

From the terminal running WireMock press `^c`.

```zsh
java -jar wiremock-jre8-standalone-2.35.0.jar --port 8888
```

#### Step 3: Configure WireMock to return a fault response

```zsh
# Fault response
curl -X POST \
  --data '{ "request": { "method": "GET", "url": "/slow/service" }, "response": {  "fault": "MALFORMED_RESPONSE_CHUNK" } }' \
  http://localhost:8888/__admin/mappings/new
```

#### Step 4: Perform the Demo

The `CircuitBreaker` has been configured with a `5` request window.

Perform `6` calls to the application's root path.

```zsh
# Run this 6 times
$ curl 'http://localhost:8080/' -i -X GET
```

The first `5` calls should return:

```json
{"message":"Exception handled by retry fallback"}
```

The circuit breaker is `Closed` since the `5` request threshold window has not been reached.

The sixth call should return:

```json
{"message":"Exception handled by circuit breaker fallback"}
```

The circuit breaker is `Open` and will block further calls to the remote service for a set period of time (default is
60000 ms.).

---

## Listing Metrics Names

http://localhost:8080/actuator/metrics

Response (formatted):

```json
{
    "names": [
        "application.ready.time",
        "application.started.time",
        "disk.free",
        "disk.total",
        "executor.active",
        "executor.completed",
        "executor.pool.core",
        "executor.pool.max",
        "executor.pool.size",
        "executor.queue.remaining",
        "executor.queued",
        "http.server.requests",
        "jvm.buffer.count",
        "jvm.buffer.memory.used",
        "jvm.buffer.total.capacity",
        "jvm.classes.loaded",
        "jvm.classes.unloaded",
        "jvm.gc.live.data.size",
        "jvm.gc.max.data.size",
        "jvm.gc.memory.allocated",
        "jvm.gc.memory.promoted",
        "jvm.gc.overhead",
        "jvm.gc.pause",
        "jvm.memory.committed",
        "jvm.memory.max",
        "jvm.memory.usage.after.gc",
        "jvm.memory.used",
        "jvm.threads.daemon",
        "jvm.threads.live",
        "jvm.threads.peak",
        "jvm.threads.states",
        "logback.events",
        "process.cpu.usage",
        "process.files.max",
        "process.files.open",
        "process.start.time",
        "process.uptime",
        "resilience4j.bulkhead.active.thread.count",
        "resilience4j.bulkhead.available.concurrent.calls",
        "resilience4j.bulkhead.available.thread.count",
        "resilience4j.bulkhead.core.thread.pool.size",
        "resilience4j.bulkhead.max.allowed.concurrent.calls",
        "resilience4j.bulkhead.max.thread.pool.size",
        "resilience4j.bulkhead.queue.capacity",
        "resilience4j.bulkhead.queue.depth",
        "resilience4j.bulkhead.thread.pool.size",
        "resilience4j.circuitbreaker.buffered.calls",
        "resilience4j.circuitbreaker.calls",
        "resilience4j.circuitbreaker.failure.rate",
        "resilience4j.circuitbreaker.not.permitted.calls",
        "resilience4j.circuitbreaker.slow.call.rate",
        "resilience4j.circuitbreaker.slow.calls",
        "resilience4j.circuitbreaker.state",
        "resilience4j.ratelimiter.available.permissions",
        "resilience4j.ratelimiter.waiting_threads",
        "resilience4j.retry.calls",
        "resilience4j.timelimiter.calls",
        "system.cpu.count",
        "system.cpu.usage",
        "system.load.average.1m",
        "tomcat.sessions.active.current",
        "tomcat.sessions.active.max",
        "tomcat.sessions.alive.max",
        "tomcat.sessions.created",
        "tomcat.sessions.expired",
        "tomcat.sessions.rejected"
    ]
}
```

---

## TimeLimiter

http://localhost:8080/actuator/metrics/resilience4j.timelimiter.calls

Response (formatted):

```json
{
    "name": "resilience4j.timelimiter.calls",
    "description": "The number of successful calls",
    "baseUnit": null,
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 5.0
        }
    ],
    "availableTags": [
        {
            "tag": "kind",
            "values": [
                "timeout",
                "successful",
                "failed"
            ]
        },
        {
            "tag": "name",
            "values": [
                "backendB",
                "backendA"
            ]
        }
    ]
}
```

### Successful

http://localhost:8080/actuator/metrics/resilience4j.timelimiter.calls?tag=name:backendA&tag=kind:successful

Response (formatted):

```json
{
    "name": "resilience4j.timelimiter.calls",
    "description": "The number of successful calls",
    "baseUnit": null,
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 1.0
        }
    ],
    "availableTags": []
}
```

### Failed

http://localhost:8080/actuator/metrics/resilience4j.timelimiter.calls?tag=name:backendA&tag=kind:failed

Response (formatted):

```json
{
    "name": "resilience4j.timelimiter.calls",
    "description": "The number of failed calls",
    "baseUnit": null,
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 3.0
        }
    ],
    "availableTags": []
}
```

### Timeout

http://localhost:8080/actuator/metrics/resilience4j.timelimiter.calls?tag=name:backendA&tag=kind:timeout

Response (formatted):

```json
{
    "name": "resilience4j.timelimiter.calls",
    "description": "The number of timed out calls",
    "baseUnit": null,
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 1.0
        }
    ],
    "availableTags": []
}
```

---

## Retry

http://localhost:8080/actuator/metrics/resilience4j.retry.calls

Response (formatted):

```json
{
    "name": "resilience4j.retry.calls",
    "description": "The number of failed calls after a retry attempt",
    "baseUnit": null,
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 3.0
        }
    ],
    "availableTags": [
        {
            "tag": "kind",
            "values": [
                "successful_without_retry",
                "successful_with_retry",
                "failed_with_retry",
                "failed_without_retry"
            ]
        },
        {
            "tag": "name",
            "values": [
                "backendA"
            ]
        }
    ]
}
```

### Successful Without Retry

http://localhost:8080/actuator/metrics/resilience4j.retry.calls?tag=kind:successful_without_retry

Response (formatted):

```json
{
    "name": "resilience4j.retry.calls",
    "description": "The number of successful calls without a retry attempt",
    "baseUnit": null,
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 1.0
        }
    ],
    "availableTags": [
        {
            "tag": "name",
            "values": [
                "backendA"
            ]
        }
    ]
}
```

### Successful With Retry

http://localhost:8080/actuator/metrics/resilience4j.retry.calls?tag=kind:successful_with_retry

Response (formatted):

```json
{
    "name": "resilience4j.retry.calls",
    "description": "The number of successful calls after a retry attempt",
    "baseUnit": null,
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 0.0
        }
    ],
    "availableTags": [
        {
            "tag": "name",
            "values": [
                "backendA"
            ]
        }
    ]
}
```

### Failed Without Retry

http://localhost:8080/actuator/metrics/resilience4j.retry.calls?tag=kind:failed_without_retry

Response (formatted):

```json
{
    "name": "resilience4j.retry.calls",
    "description": "The number of failed calls without a retry attempt",
    "baseUnit": null,
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 1.0
        }
    ],
    "availableTags": [
        {
            "tag": "name",
            "values": [
                "backendA"
            ]
        }
    ]
}
```

### Failed With Retry

http://localhost:8080/actuator/metrics/resilience4j.retry.calls?tag=kind:failed_with_retry

Response (formatted):

```json
{
    "name": "resilience4j.retry.calls",
    "description": "The number of failed calls after a retry attempt",
    "baseUnit": null,
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 1.0
        }
    ],
    "availableTags": [
        {
            "tag": "name",
            "values": [
                "backendA"
            ]
        }
    ]
}
```

---

## CircuitBreaker

### Successful

http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.calls?tag=kind:successful

Response (formatted):

```json
{
    "name": "resilience4j.circuitbreaker.calls",
    "description": "Total number of successful calls",
    "baseUnit": "seconds",
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 1.0
        },
        {
            "statistic": "TOTAL_TIME",
            "value": 0.342331358
        },
        {
            "statistic": "MAX",
            "value": 0.342331358
        }
    ],
    "availableTags": [
        {
            "tag": "name",
            "values": [
                "backendA"
            ]
        }
    ]
}
```

### Failed

http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.calls?tag=kind:failed

Response (formatted):

```json
{
    "name": "resilience4j.circuitbreaker.calls",
    "description": "Total number of failed calls",
    "baseUnit": "seconds",
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 4.0
        },
        {
            "statistic": "TOTAL_TIME",
            "value": 8.123336913
        },
        {
            "statistic": "MAX",
            "value": 2.061317937
        }
    ],
    "availableTags": [
        {
            "tag": "name",
            "values": [
                "backendA"
            ]
        }
    ]
}
```

### Not Permitted Calls

http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.not.permitted.calls?tag=kind:not_permitted

Response (formatted):

```json
{
    "name": "resilience4j.circuitbreaker.not.permitted.calls",
    "description": "Total number of not permitted calls",
    "baseUnit": null,
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 1.0
        }
    ],
    "availableTags": [
        {
            "tag": "name",
            "values": [
                "backendA"
            ]
        }
    ]
}
```

---

## Prometheus

http://localhost:8080/actuator/prometheus

Above is the endpoint used by [Prometheus](https://prometheus.io) to collect all metrics data, including the metrics
generated by Resilience4J.
