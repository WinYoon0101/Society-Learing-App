package com.example.frontend.data.remote;

import com.example.frontend.data.model.Post;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class PostDeserializer implements JsonDeserializer<Post> {

    private static final Gson PLAIN_GSON = new Gson();

    @Override
    public Post deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json == null || json.isJsonNull() || !json.isJsonObject()) {
            return null;
        }

        JsonObject obj = json.getAsJsonObject().deepCopy();
        sanitizeUserRef(obj, "authorId");
        sanitizeStringRef(obj, "groupId");
        sanitizeUserList(obj, "tags");

        return PLAIN_GSON.fromJson(obj, Post.class);
    }

    private static void sanitizeUserList(JsonObject obj, String fieldName) {
        JsonElement value = obj.get(fieldName);
        if (value == null || value.isJsonNull()) {
            return;
        }

        JsonArray sanitized = new JsonArray();
        if (value.isJsonArray()) {
            for (JsonElement item : value.getAsJsonArray()) {
                sanitized.add(toUserRef(item));
            }
        } else {
            sanitized.add(toUserRef(value));
        }
        obj.add(fieldName, sanitized);
    }

    private static void sanitizeUserRef(JsonObject obj, String fieldName) {
        JsonElement value = obj.get(fieldName);
        if (value == null || value.isJsonNull() || value.isJsonObject()) {
            return;
        }
        obj.add(fieldName, toUserRef(value));
    }

    private static void sanitizeStringRef(JsonObject obj, String fieldName) {
        JsonElement value = obj.get(fieldName);
        if (value == null || value.isJsonNull() || !value.isJsonObject()) {
            return;
        }

        String id = getObjectId(value.getAsJsonObject());
        if (id == null || id.trim().isEmpty()) {
            obj.add(fieldName, JsonNull.INSTANCE);
        } else {
            obj.addProperty(fieldName, id);
        }
    }

    private static JsonElement toUserRef(JsonElement value) {
        if (value == null || value.isJsonNull() || value.isJsonObject()) {
            return value == null ? JsonNull.INSTANCE : value;
        }

        if (value.isJsonPrimitive()) {
            String id = value.getAsString();
            if (id != null && !id.trim().isEmpty()) {
                JsonObject ref = new JsonObject();
                ref.addProperty("_id", id);
                return ref;
            }
        }
        return JsonNull.INSTANCE;
    }

    private static String getObjectId(JsonObject obj) {
        if (obj == null) {
            return null;
        }
        String id = getString(obj, "_id");
        return id != null ? id : getString(obj, "id");
    }

    private static String getString(JsonObject obj, String fieldName) {
        JsonElement value = obj.get(fieldName);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return null;
        }
        return value.getAsString();
    }
}
