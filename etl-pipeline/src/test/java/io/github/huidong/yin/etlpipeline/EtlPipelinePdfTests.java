package io.github.huidong.yin.etlpipeline;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
public class EtlPipelinePdfTests {

    @Test
    public void testPdf() throws Exception {
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                "classpath:/code.pdf",
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(
                                ExtractedTextFormatter.builder()
                                        .withNumberOfTopTextLinesToDelete(0)
                                        .build()
                        )
                        .withPagesPerDocument(1)
                        .build()
        );

        List<Document> documents = pdfReader.read();
        for (Document document : documents) {
            log.info(document.toString());
        }
    }

    @Test
    public void testPdf2() throws Exception {
        ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader("classpath:/code2.pdf",
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0)
                                .build())
                        .withPagesPerDocument(1)
                        .build());

        List<Document> documents = pdfReader.read();
        for (Document document : documents) {
            log.info(document.toString());
        }
    }
}
