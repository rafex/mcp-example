package dev.rafex.mcpexample.agents.java.etherbrain;

import dev.rafex.ether.ai.core.chat.AiChatModel;
import dev.rafex.ether.ai.core.chat.AiChatRequest;
import dev.rafex.ether.ai.core.chat.AiChatResponse;
import dev.rafex.ether.ai.core.message.AiMessage;
import dev.rafex.etherbrain.ports.model.FinalAnswer;
import dev.rafex.etherbrain.ports.model.Message;
import dev.rafex.etherbrain.ports.model.ModelClient;
import dev.rafex.etherbrain.ports.model.ModelRequest;
import dev.rafex.etherbrain.ports.model.ModelResponse;
import dev.rafex.etherbrain.ports.model.ToolRequest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeepSeekEtherBrainModelClient implements ModelClient {
    private static final Pattern TOOL_RESPONSE =
        Pattern.compile("TOOL:\\s*([^\\r\\n]+)\\R+ARGS:\\s*(.*)", Pattern.DOTALL);

    private final AiChatModel chatModel;
    private final String modelName;

    public DeepSeekEtherBrainModelClient(AiChatModel chatModel, String modelName) {
        this.chatModel = chatModel;
        this.modelName = modelName;
    }

    @Override
    public ModelResponse generate(ModelRequest request) throws Exception {
        AiChatResponse response = chatModel.generate(
            new AiChatRequest(modelName, toAiMessages(request.messages()), 0.0d, 512)
        );
        String text = normalizeResponse(response.text());

        if (text.startsWith("FINAL:")) {
            return new FinalAnswer(text.substring("FINAL:".length()).trim());
        }

        Matcher matcher = TOOL_RESPONSE.matcher(text);
        if (matcher.find()) {
            return new ToolRequest(matcher.group(1).trim(), matcher.group(2).trim());
        }

        return new FinalAnswer(text);
    }

    private static List<AiMessage> toAiMessages(List<Message> messages) {
        return messages.stream()
            .map(DeepSeekEtherBrainModelClient::toAiMessage)
            .toList();
    }

    private static AiMessage toAiMessage(Message message) {
        return switch (message.role()) {
            case SYSTEM -> AiMessage.system(message.content());
            case USER -> AiMessage.user(message.content());
            case ASSISTANT -> AiMessage.assistant(message.content());
            case TOOL -> AiMessage.user("Tool result:\n" + message.content());
        };
    }

    private static String normalizeResponse(String responseText) {
        String trimmed = responseText == null ? "" : responseText.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            String withoutFence = trimmed.substring(3, trimmed.length() - 3).trim();
            int newline = withoutFence.indexOf('\n');
            if (newline >= 0 && !withoutFence.substring(0, newline).contains(":")) {
                return withoutFence.substring(newline + 1).trim();
            }
            return withoutFence;
        }
        return trimmed;
    }
}
