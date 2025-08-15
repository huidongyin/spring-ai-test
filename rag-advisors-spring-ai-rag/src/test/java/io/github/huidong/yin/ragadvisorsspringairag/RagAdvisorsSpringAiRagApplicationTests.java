package io.github.huidong.yin.ragadvisorsspringairag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@SpringBootTest
@Slf4j
class RagAdvisorsSpringAiRagApplicationTests {

    @Qualifier("deepSeekChatModel")
    @Autowired
    private ChatModel deepSeekChatModel;
    @Autowired
    private ElasticsearchVectorStore elasticsearchVectorStore;

    public void initVectorStore() {
        List<Document> documents = List.of(
                new Document("对于单身（未婚）青年，每个人每月必须缴纳一千块的单身税。", Map.of("Time", "2025.10")),
                new Document("对于已婚未育青年，每个人每月必须缴纳一千块的已婚未育税。", Map.of("Time", "2025.09")),
                new Document("对于上班的人员，每个人每月必须缴纳一千块作为个人工作税。", Map.of("Time", "2025.08")),
                new Document("对于所有工人和农民的年总收入，要缴纳百分之四十五的个税。", Map.of("Time", "2026.01")));
        elasticsearchVectorStore.add(documents);
    }

    @Test
    void testNaiveRAG() {
        initVectorStore();
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.30)
                        .vectorStore(elasticsearchVectorStore)
                        .build())
                .build();

        String answer = ChatClient.builder(deepSeekChatModel).build().prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user("2026年，单身青年在中国上班，月薪3000，一年需要缴纳多少税务？")
                .call()
                .content();
        log.info("answer: {}", answer);
    }

    //默认情况下，RetrievalAugmentationAdvisor 不允许检索到的上下文为空。发生这种情况时，它会指示模型不要回答用户查询。可以按如下方式允许空上下文。
    @Test
    void testNaiveRAGWithQueryAugmenter() {
        initVectorStore();
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(elasticsearchVectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();

        String answer = ChatClient.builder(deepSeekChatModel).build().prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user("水浒传发生在什么朝代？")
                .call()
                .content();
        log.info("answer: {}", answer);
    }

    @Test
    void testNaiveRAGWithFilterExpression() {
        initVectorStore();
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.20)
                        .vectorStore(elasticsearchVectorStore)
                        .build()
                )
                .build();

        String answer = ChatClient.builder(deepSeekChatModel).build()
                .prompt()
                .advisors(retrievalAugmentationAdvisor)
                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "Time >= '2025.10'"))
                .user("2026年，单身青年在中国上班，月薪3000，一年需要缴纳多少税务？")
                .call()
                .content();
        log.info("answer: {}", answer);
    }

    @Test
    void testAdvancedRAG() {
        initVectorStore();
        ChatClient chatClient = ChatClient.builder(deepSeekChatModel).build();

        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(RewriteQueryTransformer.builder()
                        .chatClientBuilder(chatClient.mutate())
                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.20)
                        .vectorStore(elasticsearchVectorStore)
                        .build())
                .build();

        String answer = chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user("2026年，单身青年在中国上班，月薪3000，一年需要缴纳多少税务？")
                .call()
                .content();
        log.info("answer: {}", answer);
    }


    //Pre-Retrieval  预检索
    @Test
    void testQueryTransformationOfCompressionQueryTransformer() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(deepSeekChatModel);
        Query query = Query.builder()
                .text("And what is its second largest city?")
                .history(new UserMessage("What is the capital of Denmark?"),
                        new AssistantMessage("Copenhagen is the capital of Denmark."))
                .build();

        QueryTransformer queryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        Query transformedQuery = queryTransformer.transform(query);
        log.info("transformedQuery -> text : {} , history : {}", transformedQuery.text(), transformedQuery.history());
    }

    @Test
    void testQueryTransformationOfRewriteQueryTransformer() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(deepSeekChatModel);
        Query query = new Query("I'm studying machine learning. What is an LLM?");

        QueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        Query transformedQuery = queryTransformer.transform(query);

        log.info("transformedQuery -> text : {} ", transformedQuery.text());
    }

    @Test
    void testQueryTransformationOfTranslationQueryTransformer() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(deepSeekChatModel);
        Query query = new Query("Hvad er Danmarks hovedstad?");

        QueryTransformer queryTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetLanguage("Chinese")
                .build();

        Query transformedQuery = queryTransformer.transform(query);

        log.info("transformedQuery -> text : {} ", transformedQuery.text());
    }

    @Test
    void testQueryTransformationOfMultiQueryExpander() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(deepSeekChatModel);
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                //默认情况下，MultiQueryExpander 在展开的查询列表中包含原始查询。
                .includeOriginal(false)
                .numberOfQueries(3)
                .build();
        List<Query> queries = queryExpander.expand(new Query("How to run a Spring Boot app?"));
        for (Query query : queries) {
            log.info("query -> text : {} ", query.text());
        }
    }

    //Retrieval  检索
    @Test
    void testQueryOfVectorStoreDocumentRetriever() {
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(elasticsearchVectorStore)
                .similarityThreshold(0.73)
                .topK(5)
                .filterExpression(new FilterExpressionBuilder()
                        .eq("genre", "fairytale")
                        .build())
                .build();
        List<Document> documents = retriever.retrieve(new Query("What is the main character of the story?"));

    }

    @Test
    void testQueryOfVectorStoreDocumentRetrieverWithDynamicExpression() {
        Supplier<String> supplier = () -> "fairytale";
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(elasticsearchVectorStore)
                .filterExpression(() -> new FilterExpressionBuilder()
                        .eq("tenant", supplier)
                        .build())
                .build();
        List<Document> documents = retriever.retrieve(new Query("What are the KPIs for the next semester?"));

    }

    @Test
    void testQueryOfExpressionFilter() {
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(elasticsearchVectorStore)
                .similarityThreshold(0.73)
                .topK(5)
                .filterExpression(new FilterExpressionBuilder()
                        .eq("genre", "fairytale")
                        .build())
                .build();

        Query query = Query.builder()
                .text("Who is Anacletus?")
                .context(Map.of(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "location == 'Whispering Woods'"))
                .build();
        List<Document> retrievedDocuments = retriever.retrieve(query);

    }

    //Post-Retrieval  检索后
    @Test
    void testOfContextualQueryAugmenter(){
        QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();
    }


}
