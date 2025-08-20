package io.github.huidong.yin.etlpipeline;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.util.List;

@SpringBootTest
@Slf4j
class EtlPipelineApplicationTests {

    @Value("classpath:bikes.json")
    private Resource resource;

    @Value("classpath:book.json")
    private Resource bookResources;

    @Test
    void testLoadJson() {
        JsonReader jsonReader = new JsonReader(this.resource, "name", "email");
        List<Document> documents = jsonReader.get();
        for (Document document : documents) {
            log.info("doc : {}", document.toString());
        }

    }

    @Test
    void testJsonPointer(){
        JsonReader jsonReader = new JsonReader(bookResources);
        List<Document> documents = jsonReader.get("/0/author/0");
        for (Document document : documents) {
            log.info("doc : {}", document.toString());
        }
    }
}
