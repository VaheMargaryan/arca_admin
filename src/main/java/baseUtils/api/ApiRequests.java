package baseUtils.api;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

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
     * Payload: [1118]
     */
    public void deleteDictionaries(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;

        RequestOptions options = RequestOptions.create()
                .setData(ids)
                .setHeader("Content-Type", "application/json");

        // В твоей версии Playwright Java нет setHeaders(Map) -> ставим по одному
        for (Map.Entry<String, String> e : defaultHeaders.entrySet()) {
            options.setHeader(e.getKey(), e.getValue());
        }

        APIResponse resp = request.delete(
                apiBaseUrl + "/api/CommunicationDictionary/deleteDictionaries",
                options
        );

        if (resp.status() != 200) {
            String body;
            try {
                body = resp.text();
            } catch (Exception ex) {
                body = "<cannot read body>";
            }
            throw new IllegalStateException(
                    "Delete dictionaries failed. Status=" + resp.status() + ", Body=" + body
            );
        }
    }
}
