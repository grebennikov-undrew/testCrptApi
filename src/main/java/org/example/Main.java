package org.example;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main
{
    public static void main( String[] args ) {
        // Создать описание
        CrptApi.Document.Description description = new CrptApi.Document.Description();
        description.setParticipantInn("1234567890");

        // Создать продукт
        CrptApi.Document.Product product = new CrptApi.Document.Product();
        product.setCertificateDocument("Certificate123");
        product.setCertificateDocumentDate(LocalDate.of(2023, 5, 15));
        product.setCertificateDocumentNumber("Cert123456");
        product.setOwnerInn("0987654321");
        product.setProducerInn("5432109876");
        product.setProductionDate(LocalDate.of(2023, 6, 20));
        product.setTnvedCode("1234567890");
        product.setUitCode("UIT123");
        product.setUituCode("UITU567");

        // Создать список продуктов
        List<CrptApi.Document.Product> products = new ArrayList<>();
        products.add(product);

        // Создать документ
        CrptApi.Document document = new CrptApi.Document();
        document.setDescription(description);
        document.setDocId("12345");
        document.setDocStatus("Draft");
        document.setDocType("TypeA");
        document.setImportRequest(true);
        document.setOwnerInn("0987654321");
        document.setParticipantInn("1234567890");
        document.setProducerInn("5432109876");
        document.setProductionDate(LocalDate.of(2023, 7, 10));
        document.setProductionType("TypeX");
        document.setProducts(products);
        document.setRegDate("2023-05-30");
        document.setRegNumber("Reg12345");

        // Отправить документ
        CrptApi ds = new CrptApi(TimeUnit.SECONDS, 5);
        ds.trySend(document,"222222222");
    }
}
