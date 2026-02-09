package baseUtils.api;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiRequests {

    private final APIRequestContext request;
    private final String apiBaseUrl;
    private final Map<String, String> defaultHeaders;

    public ApiRequests(APIRequestContext request, String apiBaseUrl, Map<String, String> defaultHeaders) {
        this.request = request;
        this.apiBaseUrl = apiBaseUrl;
        this.defaultHeaders = defaultHeaders != null ? defaultHeaders : Map.of();
    }

    /**
     * DELETE /api/CommunicationDictionary/deleteDictionaries
     */
    public void deleteDictionaries(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;

        RequestOptions options = RequestOptions.create()
                .setData(ids)
                .setHeader("Content-Type", "application/json");

        for (Map.Entry<String, String> e : defaultHeaders.entrySet()) {
            options.setHeader(e.getKey(), e.getValue());
        }

        APIResponse resp = request.delete(
                apiBaseUrl + "/api/CommunicationDictionary/deleteDictionaries",
                options
        );

        if (resp.status() != 200) {
            throw new IllegalStateException("Delete dictionaries failed. Status=" + resp.status() + ", Body=" + safeBody(resp));
        }
    }

    /**
     * POST /api/CommunicationDictionary/addDictionaries
     * Важно: API принимает МАССИВ элементов.
     */
    public void addDictionaries(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) return;

        RequestOptions options = RequestOptions.create()
                .setData(items)
                .setHeader("Content-Type", "application/json");

        for (Map.Entry<String, String> e : defaultHeaders.entrySet()) {
            options.setHeader(e.getKey(), e.getValue());
        }

        APIResponse resp = request.post(
                apiBaseUrl + "/api/CommunicationDictionary/addDictionaries",
                options
        );

        if (resp.status() != 200) {
            throw new IllegalStateException("Add dictionaries failed. Status=" + resp.status() + ", Body=" + safeBody(resp));
        }
    }

    /**
     * Удобный хелпер: создать один элемент.
     */
    public void addDictionaryItem(String keyId, long entryId, int langId, String value, int behavior) {
        Map<String, Object> item = new HashMap<>();
        item.put("keyId", keyId);
        item.put("entryId", entryId);
        item.put("langId", langId);
        item.put("value", value);
        item.put("behavior", behavior);

        addDictionaries(List.of(item));
    }

    private String safeBody(APIResponse resp) {
        try {
            return resp.text();
        } catch (Exception e) {
            return "<cannot read body>";
        }
    }
}
