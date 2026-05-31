package com.algaworks.algashop.product.catalog.infrastructure.persistence.dataload;

import com.algaworks.algashop.product.catalog.infrastructure.utility.AlgaShopResourceUtils;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements ApplicationRunner {

    private final MongoOperations mongoOperations;
    private final DataLoadProperties dataLoadProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!dataLoadProperties.getEnabled()) {
            log.info("Data load is disabled. Skipping data loading process.");
            return;
        }
         log.info("Data load started");
        if (dataLoadProperties.getEnabled() == null) {
            log.warn("No sources configured for data load. Skipping data loading process.");
            return;
        }
        dataLoadProperties.getSources().forEach(this::importJsonFileToCollection);
    }

    private void importJsonFileToCollection(DataLoadProperties.DataLoadSource source) {
            log.info("Importing data from JSON file to MongoDB collection");
            String rawJson = AlgaShopResourceUtils.readContent(source.getLocation());
            if (StringUtils.isBlank(rawJson)) {
                log.warn("Resouce {} is empty", source.getLocation());
                return;
            }

        List<Document> docs = this.parseJsonDocuments(rawJson);
        int inserted  = insertInto(docs,source.getCollection());
        log.info("{} - Imports: {}/{}", source.getLocation(), inserted, docs.size());
    }

    private List<Document> parseJsonDocuments(String rawJson) {
        try {
            BsonArray array = BsonArray.parse(rawJson);
            return array.stream().map(Objects::toString).map(Document::parse).toList();
        } catch (Exception e) {
            log.error("Error parsing JSON content", e);
           return Collections.emptyList();
        }
    }

    private int insertInto(List<Document> mongoDocs, @NotBlank String collectionName) {
        if (mongoDocs == null || mongoDocs.isEmpty()) {
            return 0;
        }

        try {
            if (Boolean.TRUE.equals(dataLoadProperties.getAutoDelete())) {
                mongoOperations.getCollection(collectionName).deleteMany(new BsonDocument());
            }
            return mongoOperations.insert(mongoDocs, collectionName).size();
        } catch (Exception e) {
            log.error("Error inserting documents into {}: {}", collectionName, e.getMessage(), e);
        }

        return 0;
    }
}
