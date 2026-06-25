package com.example.frontend.data.remote;

import com.example.frontend.data.model.Message;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Field {@code Message.replyTo} có thể về dạng OBJECT (đã populate) HOẶC STRING (ObjectId
 * chưa populate — ví dụ {@code lastMessage} trong GET /conversations, hay replyTo lồng nhau
 * mà BE chỉ populate 1 cấp). Gson mặc định kỳ vọng OBJECT cho field kiểu {@link Message} →
 * ném "Expected BEGIN_OBJECT but was STRING" làm hỏng cả response.
 *
 * Deserializer này đệ quy biến mọi {@code replyTo} không-phải-object thành null rồi parse
 * bằng Gson thường (không gắn adapter này, tránh đệ quy vô hạn).
 */
public class MessageDeserializer implements JsonDeserializer<Message> {

    private static final Gson PLAIN_GSON = new Gson();

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json == null || json.isJsonNull() || !json.isJsonObject()) {
            return null;
        }
        JsonObject obj = json.getAsJsonObject();
        sanitizeReplyTo(obj);
        return PLAIN_GSON.fromJson(obj, Message.class);
    }

    /** replyTo không phải JsonObject → JsonNull; đồng thời đi sâu vào mọi object/array con. */
    private static void sanitizeReplyTo(JsonElement el) {
        if (el == null) return;
        if (el.isJsonArray()) {
            for (JsonElement child : el.getAsJsonArray()) {
                sanitizeReplyTo(child);
            }
        } else if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            JsonElement reply = obj.get("replyTo");
            if (reply != null && !reply.isJsonObject()) {
                obj.add("replyTo", JsonNull.INSTANCE);
            }
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                sanitizeReplyTo(entry.getValue());
            }
        }
    }
}
