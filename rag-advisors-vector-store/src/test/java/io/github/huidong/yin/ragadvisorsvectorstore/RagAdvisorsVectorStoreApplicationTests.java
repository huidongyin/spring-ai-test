package io.github.huidong.yin.ragadvisorsvectorstore;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
@Slf4j
class RagAdvisorsVectorStoreApplicationTests {

    @Qualifier("deepSeekChatModel")
    @Autowired
	private ChatModel deepSeekChatModel;

//	@Autowired
//	private OllamaEmbeddingModel ollamaEmbeddingModel;

	//ES_JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./bin/elasticsearch -E discovery.type=single-node -E xpack.security.enabled=false
	@Autowired
	private ElasticsearchVectorStore elasticsearchVectorStore;

	public void initVectorStore() {
		List <Document> documents = List.of(
				new Document("对于单身（未婚）青年，每个人每月必须缴纳一千块的单身税。", Map.of("Time", "2025.10")),
				new Document("对于已婚未育青年，每个人每月必须缴纳一千块的已婚未育税。", Map.of("Time", "2025.09")),
				new Document("对于所有工人和农民的年总收入，要缴纳百分之四十五的个税。", Map.of("Time", "2026.01")));
		elasticsearchVectorStore.add(documents);
	}

	@Test
	void testQuestionAnswerAdvisor() {
		initVectorStore();
		ChatClient chatClient = ChatClient.builder(deepSeekChatModel).build();
		ChatResponse chatResponse = chatClient.prompt()
				.advisors(new QuestionAnswerAdvisor(elasticsearchVectorStore))
				.user("2026年，单身青年在中国上班，月薪3000，一年需要缴纳多少税务？")
				.call()
				.chatResponse();
		String text = chatResponse.getResult().getOutput().getText();
		log.info("ai result: {}", text);
	}

	@Test
	void testQuestionAnswerAdvisorWithSearchRequest() {
		initVectorStore();
		ChatClient chatClient = ChatClient.builder(deepSeekChatModel).build();
		ChatResponse chatResponse = chatClient.prompt()
				.advisors(QuestionAnswerAdvisor.builder(elasticsearchVectorStore).searchRequest(SearchRequest.builder().similarityThreshold(0.3d).topK(6).build()).build())
				.user("2026年，单身青年在中国上班，月薪3000，一年需要缴纳多少税务？")
				.call()
				.chatResponse();
		String text = chatResponse.getResult().getOutput().getText();
		log.info("ai result: {}", text);
	}

	@Test
	void testDynamicFilterExpressions() {
		initVectorStore();
		ChatClient chatClient = ChatClient.builder(deepSeekChatModel)
				.defaultAdvisors(QuestionAnswerAdvisor.builder(elasticsearchVectorStore).searchRequest(SearchRequest.builder().similarityThreshold(0.3d).topK(6).build()).build())
				.build();
		ChatResponse chatResponse = chatClient.prompt()
				.advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "Time >= '2025.10'"))
				.user("2026年，单身青年上班一年，月薪3000，一年需要缴纳多少税务？")
				.call()
				.chatResponse();
		String text = chatResponse.getResult().getOutput().getText();
		log.info("ai result: {}", text);
	}

	@Test
	void testCustomPromptTemplate(){
		initVectorStore();
		PromptTemplate customPromptTemplate = PromptTemplate.builder()
				.renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
				.template("""
            <query>

            Context information is below.

			---------------------
			<question_answer_context>
			---------------------

			Given the context information and no prior knowledge, answer the query.

			Follow these rules:

			1. If the answer is not in the context, just say that you don't know.
			2. Avoid statements like "Based on the context..." or "The provided information...".
            """)
				.build();

		String question = "Where does the adventure of Anacletus and Birba take place?";

		QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(elasticsearchVectorStore)
				.promptTemplate(customPromptTemplate)
				.build();

		String response = ChatClient.builder(deepSeekChatModel).build()
				.prompt(question)
				.advisors(qaAdvisor)
				.call()
				.content();
		log.info("ai result: {}", response);
	}





}
