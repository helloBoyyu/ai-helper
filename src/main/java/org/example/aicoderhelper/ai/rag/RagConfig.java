package org.example.aicoderhelper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;

import java.io.File;
import java.util.List;

@Configuration
public class RagConfig {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Bean
    public ContentRetriever contentRetriever() {

        try {
            // 加载文档
            ClassPathResource resource = new ClassPathResource("docs");
            File docDir = resource.getFile();
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(docDir.toPath());


            // 定义文档切割
            DocumentByParagraphSplitter documentByParagraphSplitter = new DocumentByParagraphSplitter(1000, 200);

            // 定义文档加载器， 将文档向量化后存储到向量数据库中
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(documentByParagraphSplitter)
                    // 将文档碎片添加文档标题， 提高文档质量
                    .textSegmentTransformer(textSegment -> TextSegment.from(textSegment.metadata().getString("file_name") +
                            "\n" + textSegment.text(), textSegment.metadata()))
                    // 使用向量模型
                    .embeddingModel(qwenEmbeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            // 加载文档
            ingestor.ingest(documents);
        } catch (Exception e) {

        }

        // 自定义内容加载器
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(qwenEmbeddingModel)
                .maxResults(5)
                .minScore(0.75)
                .build();
    }
}
