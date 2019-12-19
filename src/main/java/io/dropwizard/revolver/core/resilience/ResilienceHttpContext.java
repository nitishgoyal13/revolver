package io.dropwizard.revolver.core.resilience;

import io.dropwizard.revolver.http.RevolverHttpContext;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/***
 Created by nitish.goyal on 23/11/19
 ***/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResilienceHttpContext extends RevolverHttpContext {

    private CircuitBreaker defaultCircuitBreaker;

    private Map<String, CircuitBreaker> apiVsCircuitBreaker;

    private Map<String, Bulkhead> poolVsBulkHeadMap;

    private Map<String, Integer> apiVsTimeout;

    private ExecutorService executor = Executors.newCachedThreadPool();

}
