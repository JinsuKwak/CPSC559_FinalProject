package p2pclient.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtils {
    /**
     * Formats a valid JSON string into a readable, pretty-printed format.
     *
     * @param json the JSON string to pretty print
     * @return a pretty-printed JSON string, or an error message if the input is invalid
     */
    public static String jsonFormatter(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "Input JSON cannot be null or empty.";
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Enable pretty printing
            Object jsonObject = objectMapper.readValue(json, Object.class);
            return objectMapper.writeValueAsString(jsonObject);
        } catch (JsonProcessingException e) {
            return "Invalid JSON format.";
        } catch (Exception e) {
            return "An unexpected error occurred.";
        }
    }
}
