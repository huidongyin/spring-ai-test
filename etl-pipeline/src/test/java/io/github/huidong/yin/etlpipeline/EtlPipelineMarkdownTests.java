package io.github.huidong.yin.etlpipeline;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.util.List;

@SpringBootTest
@Slf4j
public class EtlPipelineMarkdownTests {

    @Value("classpath:code.md")
    private Resource resource;

    @Test
    public void test() throws Exception {
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", "code.md")
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(this.resource, config);
        List<Document> documents = reader.get();
        for (Document document : documents) {
            log.info(document.toString());
        }
    }


}
