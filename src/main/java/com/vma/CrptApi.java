package com.vma;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.concurrent.*;

import java.io.IOException;

public class CrptApi
{
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Semaphore semaphore;
    private int requestLimit;

    public CrptApi(int requestLimit, int intervalSeconds) {
        scheduler.scheduleAtFixedRate(this::resetRequestCount, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        this.semaphore = new Semaphore(requestLimit);
        this.requestLimit = requestLimit;
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(5, 1); // Пример: 5 запросов в секунду
        String documentJson = "{\"description\": { \"participantInn\": \"string\" }, \"doc_id\": \"string\", \"doc_status\": \"string\", \"doc_type\": \"LP_INTRODUCE_GOODS\", \"importRequest\": true, \"owner_inn\": \"string\", \"participant_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\", \"products\": [ { \"certificate_document\": \"string\", \"certificate_document_date\": \"2020-01-23\", \"certificate_document_number\": \"string\", \"owner_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"tnved_code\": \"string\", \"uit_code\": \"string\", \"uitu_code\": \"string\" } ], \"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}";
        String signature = "signature_string";
        ExecutorService executor = Executors.newFixedThreadPool(10); // Создаем пул потоков из 10 потоков
        try {
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    try {
                        api.createDocument(new JSONObject(documentJson), signature);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private void acquire() throws InterruptedException {
        semaphore.acquire();
    }

    public void createDocument(JSONObject requestBody, String signature) throws InterruptedException {
        acquire();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(API_URL);
            httpPost.setHeader("Content-Type", "application/json");

            // Добавляем параметр signature в JSON тело запроса
            requestBody.put("signature", signature);

            StringEntity requestEntity = new StringEntity(requestBody.toString());
            httpPost.setEntity(requestEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity responseEntity = response.getEntity();
                System.out.println("Response code: " + response.getStatusLine().getStatusCode());
                System.out.println("Response body: " + EntityUtils.toString(responseEntity));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetRequestCount() {
        semaphore.release(requestLimit);
    }

}
