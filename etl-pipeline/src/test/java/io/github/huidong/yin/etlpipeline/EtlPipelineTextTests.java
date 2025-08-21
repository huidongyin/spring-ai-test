package io.github.huidong.yin.etlpipeline;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.util.List;

@SpringBootTest
@Slf4j
public class EtlPipelineTextTests {

    @Value("classpath:text-source.txt")
    private Resource resource;


    @Test
    public void test() {
        TextReader textReader = new TextReader(this.resource);
        textReader.getCustomMetadata().put("filename", "text-source.txt");
        List<Document> documents = textReader.read();
        for (Document document : documents) {
            log.info(document.toString());
        }
    }


    @Test
    public void testSplitDoc(){
        TextReader textReader = new TextReader(this.resource);
        textReader.getCustomMetadata().put("filename", "text-source.txt");
        List<Document> documents = textReader.read();
        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder().withChunkSize(10).build();
        List<Document> splitDocs = tokenTextSplitter.apply(documents);
        for (Document splitDoc : splitDocs) {
            log.info(splitDoc.toString());
        }
    }


}
