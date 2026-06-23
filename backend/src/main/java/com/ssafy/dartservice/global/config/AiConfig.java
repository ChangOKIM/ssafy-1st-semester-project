package com.ssafy.dartservice.global.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean("anthropicChatClient")
    public ChatClient anthropicChatClient(AnthropicChatModel model) {
        return ChatClient.create(model);
    }

    @Bean("openaiChatClient")
    public ChatClient openaiChatClient(OpenAiChatModel model) {
        return ChatClient.create(model);
    }
}
