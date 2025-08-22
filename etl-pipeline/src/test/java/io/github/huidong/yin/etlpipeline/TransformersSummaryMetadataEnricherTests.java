package io.github.huidong.yin.etlpipeline;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
public class TransformersSummaryMetadataEnricherTests {

    @Autowired
    private ChatModel chatModel;

    @Test
    public void test() {
        SummaryMetadataEnricher enricher = new SummaryMetadataEnricher(chatModel,
                List.of(SummaryMetadataEnricher.SummaryType.PREVIOUS, SummaryMetadataEnricher.SummaryType.CURRENT, SummaryMetadataEnricher.SummaryType.NEXT));

        Document doc1 = new Document("Content of document 1");
        Document doc2 = new Document("Content of document 2");

        List<Document> enrichedDocs = enricher.apply(List.of(doc1, doc2));

        // Check the metadata of the enriched documents
        for (Document doc : enrichedDocs) {
            log.info("Current summary: {}", doc.getMetadata().get("section_summary"));
            log.info("Previous summary: {}", doc.getMetadata().get("prev_section_summary"));
            log.info("Next summary: {}", doc.getMetadata().get("next_section_summary"));
        }
    }
}
