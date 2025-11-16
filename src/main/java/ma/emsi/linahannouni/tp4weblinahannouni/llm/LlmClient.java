package ma.emsi.linahannouni.tp4weblinahannouni.llm;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.data.message.SystemMessage;

import jakarta.enterprise.context.Dependent;
import ma.emsi.linahannouni.tp4weblinahannouni.assistant.Assistant;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URISyntaxException;
import java.util.*;

@Dependent
public class LlmClient implements Serializable {

    private Assistant assistant;
    private ChatMemory memory;
    private ChatModel model;

    private String systemRole;

    public LlmClient() {

        String key = System.getenv("GEMINI_KEY");
        String tavilyKey = System.getenv("TAVILY_API_KEY");

        model = GoogleAiGeminiChatModel.builder()
                .apiKey(key)
                .modelName("gemini-2.5-flash")
                .temperature(0.2)
                .logRequestsAndResponses(true)
                .build();

        // --- Embedding model
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // --- PDF 1
        EmbeddingStore<TextSegment> store1 = new InMemoryEmbeddingStore<>();
        ingest("rag.pdf", store1, embeddingModel);

        ContentRetriever retriever1 = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(store1)
                .maxResults(3)
                .minScore(0.4)
                .build();

        // --- PDF 2
        EmbeddingStore<TextSegment> store2 = new InMemoryEmbeddingStore<>();
        ingest("Support.pdf", store2, embeddingModel);

        ContentRetriever retriever2 = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(store2)
                .maxResults(3)
                .minScore(0.4)
                .build();

        // --- Web Search (Test 5)
        WebSearchEngine tavily = TavilyWebSearchEngine.builder()
                .apiKey(tavilyKey)
                .build();

        ContentRetriever webRetriever = WebSearchContentRetriever.builder()
                .webSearchEngine(tavily)
                .maxResults(3)
                .build();

        // --- Routage LM (Test 3)
        Map<ContentRetriever, String> descriptions = new HashMap<>();
        descriptions.put(retriever1, "Document sur le RAG, IA, embeddings, LangChain4j");
        descriptions.put(retriever2, "Document sur MQL, génie logiciel, modèles conceptuels");
        descriptions.put(webRetriever, "Informations recherchées sur le Web");

        LanguageModelQueryRouter lmRouter = new LanguageModelQueryRouter(model, descriptions);

        // --- Retrieval Augmentor
        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(lmRouter)
                .build();

        memory = MessageWindowChatMemory.withMaxMessages(10);

        assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(memory)
                .retrievalAugmentor(augmentor)
                .build();
    }



    public void setSystemRole(String role) {
        this.systemRole = role;
        memory.clear();
        if (role != null && !role.isBlank()) {
            memory.add(new SystemMessage(role));
        }
    }

    public String envoyerQuestion(String question) {
        return assistant.chat(question);
    }


    // ---------------- PDF ingestion utility ---------------------
    private void ingest(String filename, EmbeddingStore<TextSegment> store, EmbeddingModel model) {
        try {
            Path path = Paths.get(Objects.requireNonNull(
                    getClass().getClassLoader().getResource(filename)
            ).toURI());
            Document doc = FileSystemDocumentLoader.loadDocument(path, new ApacheTikaDocumentParser());
            var splitter = DocumentSplitters.recursive(300, 30);
            List<TextSegment> segments = splitter.split(doc);
            List<Embedding> embeds = model.embedAll(segments).content();
            store.addAll(embeds, segments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
