# Resilience4J Demo

## Purpose

This is a Proof-of-Concept (PoC) for experimenting and demonstrating the Resilience4J library.

## Dependencies

Java: 17.0.5
Spring Boot: 2.7.6
WebFlux: 2.7.6
Lombok: 1.18.24
Resilience4J: 2.0.0
WireMock: 2.35.0

## Resilience4j

* [Resilience4j](https://resilience4j.readme.io)
* [Micrometer](https://resilience4j.readme.io/docs/micrometer)
* [Grafana](https://resilience4j.readme.io/docs/grafana-1)

## WireMock

WireMock is used to emulate external REST calls and simulate delayed responses.

Application Port: 8080
WireMock Port: 8888

* [WireMock](https://wiremock.org)
* [Getting Started](https://wiremock.org/docs/getting-started/)

### Running Tests

```zsh
./mvnw test
```

### Running WireMock Standalone

WireMock can be run as a "standalone" server. This is useful for experimenting with running the application locally.

Note: This is not required to run the test suite, and must not be running in standalone when running tests since the
demo hard-codes the WireMock Port of 8888.

The application exposes a single REST endpoint at the "root" path:

[Open in Default Browser](http://localhost:8080/)

#### Starting the WireMock server

Download and usage manual for [WireMock Standalone](https://wiremock.org/docs/running-standalone/).

```zsh
java -jar wiremock-jre8-standalone-2.35.0.jar --port 8888
```

Add one of these WireMock stub endpoints to simulate a response:

```zsh
# No delay
curl -X POST \
  --data '{ "request": { "url": "/slow/service", "method": "GET" }, "response": { "status": 200, "body": "Here it is!\n" }}' \
  http://localhost:8888/__admin/mappings/new
```

```zsh
# 3 second delay
curl -X POST \
  --data '{ "request": { "method": "GET", "url": "/slow/service" }, "response": { "status": 200, "body": "Here it is!\n", "fixedDelayMilliseconds": 3000 } }' \
  http://localhost:8888/__admin/mappings/new
```

```zsh
# Fault response
curl -X POST \
  --data '{ "request": { "method": "GET", "url": "/slow/service" }, "response": {  "fault": "MALFORMED_RESPONSE_CHUNK" } }' \
  http://localhost:8888/__admin/mappings/new
```

## Metrics & Prometheus

### Listing Metrics Names

Get a list of all available metrics names using the following command in a terminal, or visit the URL in a web browser:

* [/actuator/metrics](http://localhost:8080/actuator/metrics)

```zsh
$ curl 'http://localhost:8080/actuator/metrics' -i -X GET
```

### Querying Metrics by Name

This demo currently provides metrics for resilience4j's `TimeLimiter`. Get the details of the metric with the following
command, or visiting the URL in a web browser:

* [](http://localhost:8080/actuator/metrics/resilience4j.timelimiter.calls)

```zsh
$ curl 'http://localhost:8080/actuator/metrics/resilience4j.timelimiter.calls' -i -X GET
```

Response:

```json
{
  "name": "resilience4j.timelimiter.calls",
  "description": "The number of successful calls",
  "baseUnit": null,
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 18.0
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

### Drilling into Metrics Using Tags

Use tags to drill into the details provided by the resilience4j `TimeLimiter` using the following command, or visit the
URL in a web browser:

```zsh
$ curl 'http://localhost:8080/actuator/metrics/resilience4j.timelimiter.calls?tag=name:backendA&tag=kind:timeout' -i -X GET
```

Example Response (timeouts):

```json
{
    "name": "resilience4j.timelimiter.calls",
    "description": "The number of timed out calls",
    "baseUnit": null,
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 2.0
        }
    ],
    "availableTags": []
}
```

### Prometheus

```zsh
$ curl 'http://localhost:8080/actuator/prometheus' -i -X GET
```
