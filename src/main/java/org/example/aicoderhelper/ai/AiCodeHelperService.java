package org.example.aicoderhelper.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import org.example.aicoderhelper.ai.guardrails.SafeInputGuardrail;
import reactor.core.publisher.Flux;

import java.util.List;

@InputGuardrails({ SafeInputGuardrail.class  })
public interface AiCodeHelperService {

    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String userMessage);

    // 结构化输出
    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatForReport(String userMessage);

    record Report(String name, List<String> suggestionList, String questions) {}

    // 返回增加了引用原文档
    @SystemMessage(fromResource = "system-prompt.txt")
    Result<String> chatWithRag(String userMessage);

    // 流式输出
    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String message);
}
