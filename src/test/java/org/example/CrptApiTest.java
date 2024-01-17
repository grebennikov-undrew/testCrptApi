package org.example;

import junit.framework.TestCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApiTest extends TestCase {

    public void LimitTest() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CrptApi sender = new CrptApi(TimeUnit.MINUTES, 3);
        AtomicInteger successfulRequests = new AtomicInteger();

        for (int i = 0; i < numberOfThreads; i++) {
            ((ExecutorService) service).submit(() -> {
                CrptApi.Document document = new CrptApi.Document();
                sender.trySend(document, "signature");
                successfulRequests.incrementAndGet();
            });
        }

        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);

        // Проверка, что за указанный промежуток не прошло более 3х запросов
        assertTrue(successfulRequests.get() <= 3);
    }



}