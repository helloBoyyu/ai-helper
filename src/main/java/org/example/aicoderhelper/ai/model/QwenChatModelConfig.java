package org.example.aicoderhelper.ai.model;

import dev.langchain4j.community.dashscope.spring.ChatModelProperties;
import dev.langchain4j.community.dashscope.spring.Properties;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class QwenChatModelConfig {

    @Resource
    ChatModelListener chatModelListener;

    @Bean
    @ConditionalOnProperty({"langchain4j.community.dashscope.chat-model.api-key"})
    public ChatModel myQwenChatModel(Properties properties) {
        ChatModelProperties chatModelProperties = properties.getChatModel();
        return QwenChatModel.builder()
                .modelName(chatModelProperties.getModelName())
                .apiKey(chatModelProperties.getApiKey())
                .listeners(List.of(chatModelListener))
                .build();
    }
}
