package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Класс для работы с API Честного знака
 * Пример вызова:
 * CrptApi ds = new CrptApi(TimeUnit.SECONDS, 2);
 * ds.trySend(document,"2");
 */
class CrptApi {

    // Адрес API Честного знака для создания документов
    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    // Объект для сериализации/десереализации
    private static final ObjectMapper mapper = initMapper();
    // Ограничитель запросов (реализован ниже)
    private final SlidingRateLimiter rateLimiter;
    // Лимит запросов на единицу времени
    private final int requestLimit;
    // Единица времени (секунды, минуты...)
    private final TimeUnit timeUnit;

    // Задает настройки для сериализации
    // Формат даты: "2023-01-23"
    private static ObjectMapper initMapper() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(dateFormat);
        return objectMapper;
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        // Запретить использование временных интервалов меньше миллисекунды
        if (timeUnit == TimeUnit.NANOSECONDS) {
            throw new IllegalArgumentException("Argument TimeUnit.NANOSECONDS is not allowed, use TimeUnit.MILLISECONDS instead");
        } else if (timeUnit == TimeUnit.MICROSECONDS) {
            throw new IllegalArgumentException("Argument TimeUnit.MICROSECONDS is not allowed, use TimeUnit.MILLISECONDS instead");
        }
        this.requestLimit = requestLimit;
        this.timeUnit = timeUnit;
        rateLimiter = new SlidingRateLimiter(timeUnit, requestLimit);
    }

    // Отправить документ на сервер Честного знака
    public void trySend(Document document, String signature) {
        if (rateLimiter.tryAcquire()) {
            postDocument(document);
            // В зависимости от требований можно добавить обработку ответа
        }

    }

    // Отправить пост запрос
    private void postDocument(Document document)  {
        // Отправка сообщений производится при помощи библиотеки Apache httpclient
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            // Указываем адрес и заголовки
            HttpPost httpPost = new HttpPost(URL);
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Accept-Charset", "utf-8");

            // Преобразуем в JSON
            HttpEntity requestEntity = new StringEntity(mapper.writeValueAsString(document));
            httpPost.setEntity(requestEntity);

            // Выполняем POST-запрос
            HttpResponse response = httpClient.execute(httpPost);

            // В зависимости от требований можно добавить обработку статуса ответа и вернуть тело/статус

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // Класс, реализующий RateLimiter на основе метода sliding window
    static class SlidingRateLimiter {

        // Размер временного окна в миллисекундах
        private final long windowSizeMillis;
        // Максимальное количество запросов во временном окне
        private final int requestLimit;
        // Временные метки запросов
        private final Deque<Long> requests = new ConcurrentLinkedDeque<>();
        private final Lock lock = new ReentrantLock();

        SlidingRateLimiter (TimeUnit timeUnit, int requestLimit) {
            // Размер окна - один временной интервал (в соответствии с заданием)
            // При необходимости добавления N-интервала, можно добавить новый конструктор (например, X запросов в 5 секунд)
            this.windowSizeMillis = timeUnit.toMillis(1);
            this.requestLimit = requestLimit;
        }

        // Запросить доступ
        public boolean tryAcquire() {
            lock.lock();
            try {
                // Если лимит превышен, ждать до получения доступа
                if (requests.size() >= requestLimit && System.currentTimeMillis() - requests.peekFirst() < windowSizeMillis) {
                    long delayTime = requests.peekFirst() + windowSizeMillis - System.currentTimeMillis();
                    if (delayTime > 0) {
                        Thread.sleep(delayTime);
                    }
                }

                // Удалить временные метки запросов, старше размера окна
                releaseRequests();

                // Записать текущее время доступа выдать разрешение
                requests.addLast(System.currentTimeMillis());
                return true;

            } catch (InterruptedException e) {
                return false;
            } finally {
                lock.unlock();
            }
        }

        // Удаляет временные метки запросов, старше размера окна
        private void releaseRequests() {
            while (!requests.isEmpty() && System.currentTimeMillis() - requests.peekFirst() >= windowSizeMillis) {
                requests.pollFirst();
            }
        }

    }

    // Документ, отправляемый в API честного знака
    public static class Document {
        private Description description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;
        @JsonProperty("doc_type")
        private String docType;
        @JsonProperty("importRequest")
        private boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private LocalDate productionDate;
        @JsonProperty("production_type")
        private String productionType;
        private List<Product> products;
        @JsonProperty("reg_date")
        private String regDate;
        @JsonProperty("reg_number")
        private String regNumber;

        // Геттеры и сеттеры
        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public void setDocStatus(String docStatus) {
            this.docStatus = docStatus;
        }

        public String getDocType() {
            return docType;
        }

        public void setDocType(String docType) {
            this.docType = docType;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public LocalDate getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(LocalDate productionDate) {
            this.productionDate = productionDate;
        }

        public String getProductionType() {
            return productionType;
        }

        public void setProductionType(String productionType) {
            this.productionType = productionType;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public String getRegDate() {
            return regDate;
        }

        public void setRegDate(String regDate) {
            this.regDate = regDate;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public void setRegNumber(String regNumber) {
            this.regNumber = regNumber;
        }

        // Описание документа, отправляемого в API честного знака
        public static class Description {
            private String participantInn;

            // Геттеры и сеттеры
            public String getParticipantInn() {
                return participantInn;
            }

            public void setParticipantInn(String participantInn) {
                this.participantInn = participantInn;
            }
        }

        // Продукт в документе, отправляемом в API честного знака
        public static class Product {
            @JsonProperty("certificate_document")
            private String certificateDocument;
            @JsonProperty("certificate_document_date")
            private LocalDate certificateDocumentDate;
            @JsonProperty("certificate_document_number")
            private String certificateDocumentNumber;
            @JsonProperty("owner_inn")
            private String ownerInn;
            @JsonProperty("producer_inn")
            private String producerInn;
            @JsonProperty("production_date")
            private LocalDate productionDate;
            @JsonProperty("tnved_code")
            private String tnvedCode;
            @JsonProperty("uit_code")
            private String uitCode;
            @JsonProperty("uitu_code")
            private String uituCode;

            // Геттеры и сеттеры
            public String getCertificateDocument() {
                return certificateDocument;
            }

            public void setCertificateDocument(String certificateDocument) {
                this.certificateDocument = certificateDocument;
            }

            public LocalDate getCertificateDocumentDate() {
                return certificateDocumentDate;
            }

            public void setCertificateDocumentDate(LocalDate certificateDocumentDate) {
                this.certificateDocumentDate = certificateDocumentDate;
            }

            public String getCertificateDocumentNumber() {
                return certificateDocumentNumber;
            }

            public void setCertificateDocumentNumber(String certificateDocumentNumber) {
                this.certificateDocumentNumber = certificateDocumentNumber;
            }

            public String getOwnerInn() {
                return ownerInn;
            }

            public void setOwnerInn(String ownerInn) {
                this.ownerInn = ownerInn;
            }

            public String getProducerInn() {
                return producerInn;
            }

            public void setProducerInn(String producerInn) {
                this.producerInn = producerInn;
            }

            public LocalDate getProductionDate() {
                return productionDate;
            }

            public void setProductionDate(LocalDate productionDate) {
                this.productionDate = productionDate;
            }

            public String getTnvedCode() {
                return tnvedCode;
            }

            public void setTnvedCode(String tnvedCode) {
                this.tnvedCode = tnvedCode;
            }

            public String getUitCode() {
                return uitCode;
            }

            public void setUitCode(String uitCode) {
                this.uitCode = uitCode;
            }

            public String getUituCode() {
                return uituCode;
            }

            public void setUituCode(String uituCode) {
                this.uituCode = uituCode;
            }
        }
    }

}

