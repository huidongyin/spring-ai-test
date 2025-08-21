package io.github.huidong.yin.etlpipeline;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.util.List;

@SpringBootTest
@Slf4j
public class EtlPipelineTikaTests {

    @Value("classpath:/word-sample.docx")
    private Resource resource;

    @Test
    public void test() {
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(this.resource);
        List<Document> documentList = tikaDocumentReader.read();
        for (Document document : documentList) {
            log.info(document.toString());
        }
    }
}
