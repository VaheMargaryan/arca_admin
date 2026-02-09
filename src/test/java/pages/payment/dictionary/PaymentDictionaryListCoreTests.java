package pages.payment.dictionary;

import baseUtils.api.ApiRequests;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import pages.mainPage.MenuBarPage;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class PaymentDictionaryListCoreTests {

    private Playwright playwright;
    private Browser browser;

    private BrowserContext context;
    private Page page;

    private MenuBarPage menuBarPage;
    private PaymentDictionaryListPage paymentDictionaryListPage;
    private DeletePaymentDictionaryItemsPage deletePaymentDictionaryItemsPage;

    private ApiRequests apiRequests;

    private static final String BASE_URL =
            System.getProperty("baseUrl",
                    System.getenv().getOrDefault("BASE_URL", "https://admin-web-dev.itguru.am/home"));

    private static final String API_BASE_URL =
            System.getProperty("apiBaseUrl",
                    System.getenv().getOrDefault("API_BASE_URL", "https://adminopenapi-dev.itguru.am"));

    private static final String TOAST_DELETED = "The dictionary was deleted successfully.";

    // По твоему payload на скрине: English = langId 2
    private static final int LANG_EN = 2;
    private static final int BEHAVIOR_DEFAULT = 1;

    // ===== Test data created in @BeforeEach =====
    private final List<String> createdEntryIds = new ArrayList<>();

    @BeforeAll
    void beforeAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
        );
    }

    @AfterAll
    void afterAll() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void beforeEach(TestInfo testInfo) {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1920, 1080));
        page = context.newPage();
        page.navigate(BASE_URL);

        menuBarPage = new MenuBarPage(page);
        paymentDictionaryListPage = new PaymentDictionaryListPage(page);
        deletePaymentDictionaryItemsPage = new DeletePaymentDictionaryItemsPage(page);

        apiRequests = new ApiRequests(context.request(), API_BASE_URL, buildApiHeaders());

        createdEntryIds.clear();

        // ===== Determine how many records we need for THIS test =====
        String methodName = testInfo.getTestMethod().map(Method::getName).orElse("");

        int needToCreate = switch (methodName) {
            case "deleteViaTrashIcon_shouldDeleteWithConfirmModal" -> 1;
            case "deleteSelected_one_shouldDeleteWithoutModal" -> 1;
            case "deleteSelected_two_shouldDeleteWithoutModal" -> 2;
            default -> 0;
        };

        // ===== Create records via API =====
        for (int i = 1; i <= needToCreate; i++) {
            long entryId = randomEntryIdLong();
            String entryIdStr = String.valueOf(entryId);

            apiRequests.addDictionaryItem(
                    "ProviderType",
                    entryId,
                    LANG_EN,
                    "autotest delete " + methodName + " #" + i,
                    BEHAVIOR_DEFAULT
            );

            createdEntryIds.add(entryIdStr);
        }

        // ===== Ensure records appear in UI list (otherwise delete tests are meaningless) =====
        if (!createdEntryIds.isEmpty()) {
            goToPaymentDictionaryList();
            for (String entryId : createdEntryIds) {
                paymentDictionaryListPage.waitEntryIdVisible(entryId);
                Assertions.assertTrue(paymentDictionaryListPage.isEntryIdPresent(entryId),
                        "Созданный через API entryId не найден в списке: " + entryId);
            }
        }
    }

    @AfterEach
    void afterEach() {
        // Safety cleanup: if something was not deleted by UI (test failed), delete via API
        try {
            if (!createdEntryIds.isEmpty()) {
                goToPaymentDictionaryList();

                List<Integer> idsToDelete = new ArrayList<>();
                for (String entryId : createdEntryIds) {
                    if (paymentDictionaryListPage.isEntryIdPresent(entryId)) {
                        Integer id = paymentDictionaryListPage.getDictionaryIdByEntryId(entryId);
                        if (id != null) idsToDelete.add(id);
                    }
                }

                if (!idsToDelete.isEmpty()) {
                    apiRequests.deleteDictionaries(idsToDelete);
                }
            }
        } catch (Exception ignored) {
            // cleanup не должен валить прогон
        } finally {
            if (context != null) context.close();
        }
    }

    // ===== helpers =====

    private void goToPaymentDictionaryList() {
        menuBarPage.clickPaymentDictionary();
        assertThat(page).hasURL(Pattern.compile(".*/payment/dictionary/list.*"));
        paymentDictionaryListPage.waitOpened();
    }

    private void assertToast(String expectedText) {
        Locator msg = page.locator(
                        "mat-snack-bar-container, " +
                                "simple-snack-bar, " +
                                "[role='status'], " +
                                "[aria-live='polite']"
                )
                .filter(new Locator.FilterOptions().setHasText(expectedText))
                .first();

        msg.waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE)
                .setTimeout(10_000));

        assertThat(msg).containsText(expectedText);
    }

    private long randomEntryIdLong() {
        return ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
    }

    private Map<String, String> buildApiHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Origin", "https://admin-web-dev.itguru.am");

        try {
            Object tokenObj = page.evaluate("() => " +
                    "localStorage.getItem('access_token') || " +
                    "localStorage.getItem('accessToken') || " +
                    "localStorage.getItem('token') || " +
                    "sessionStorage.getItem('access_token') || " +
                    "sessionStorage.getItem('accessToken') || " +
                    "sessionStorage.getItem('token')"
            );

            if (tokenObj != null) {
                String token = tokenObj.toString().trim();
                if (!token.isEmpty() && !token.equals("null")) {
                    headers.put("Authorization", "Bearer " + token);
                }
            }
        } catch (Exception ignored) {}

        return headers;
    }

    // ===== TESTS =====

    @Test
    @Order(1)
    @DisplayName("Payment Dictionary List -> delete via trash icon")
    void deleteViaTrashIcon_shouldDeleteWithConfirmModal() {
        Assertions.assertEquals(1, createdEntryIds.size(), "Ожидали 1 тестовую запись для удаления");
        String entryId = createdEntryIds.get(0);

        goToPaymentDictionaryList();
        paymentDictionaryListPage.waitEntryIdVisible(entryId);

        paymentDictionaryListPage.clickTrashDeleteByEntryId(entryId);
        paymentDictionaryListPage.confirmDeleteModal();

        assertToast(TOAST_DELETED);

        // Проверяем, что удалили именно созданный entryId
        paymentDictionaryListPage.waitEntryIdDisappears(entryId);
        Assertions.assertFalse(paymentDictionaryListPage.isEntryIdPresent(entryId),
                "Запись всё ещё существует после удаления (trash). EntryId=" + entryId);
    }

    @Test
    @Order(2)
    @DisplayName("Payment Dictionary List -> delete selected (1 checkbox)")
    void deleteSelected_one_shouldDeleteWithoutModal() {
        Assertions.assertEquals(1, createdEntryIds.size(), "Ожидали 1 тестовую запись для удаления");
        String entryId = createdEntryIds.get(0);

        goToPaymentDictionaryList();
        paymentDictionaryListPage.waitEntryIdVisible(entryId);

        paymentDictionaryListPage.selectRowByEntryId(entryId);
        paymentDictionaryListPage.clickDeleteSelected();

        deletePaymentDictionaryItemsPage.waitOpened();
        Assertions.assertEquals(1, deletePaymentDictionaryItemsPage.rowsCount(),
                "Ожидали 1 строку на странице удаления");

        deletePaymentDictionaryItemsPage.clickDelete();
        assertToast(TOAST_DELETED);
        deletePaymentDictionaryItemsPage.waitEmptyState();

        // Проверяем, что удалили именно созданный entryId
        goToPaymentDictionaryList();
        paymentDictionaryListPage.waitEntryIdDisappears(entryId);
        Assertions.assertFalse(paymentDictionaryListPage.isEntryIdPresent(entryId),
                "Запись всё ещё существует после удаления (delete selected 1). EntryId=" + entryId);
    }

    @Test
    @Order(3)
    @DisplayName("Payment Dictionary List -> delete selected (2 checkboxes)")
    void deleteSelected_two_shouldDeleteWithoutModal() {
        Assertions.assertEquals(2, createdEntryIds.size(), "Ожидали 2 тестовые записи для удаления");
        String entryId1 = createdEntryIds.get(0);
        String entryId2 = createdEntryIds.get(1);

        goToPaymentDictionaryList();
        paymentDictionaryListPage.waitEntryIdVisible(entryId1);
        paymentDictionaryListPage.waitEntryIdVisible(entryId2);

        paymentDictionaryListPage.selectRowByEntryId(entryId1);
        paymentDictionaryListPage.selectRowByEntryId(entryId2);
        paymentDictionaryListPage.clickDeleteSelected();

        deletePaymentDictionaryItemsPage.waitOpened();
        Assertions.assertEquals(2, deletePaymentDictionaryItemsPage.rowsCount(),
                "Ожидали 2 строки на странице удаления");

        deletePaymentDictionaryItemsPage.clickDelete();
        assertToast(TOAST_DELETED);
        deletePaymentDictionaryItemsPage.waitEmptyState();

        // Проверяем, что удалили именно созданные entryId
        goToPaymentDictionaryList();
        paymentDictionaryListPage.waitEntryIdDisappears(entryId1);
        paymentDictionaryListPage.waitEntryIdDisappears(entryId2);

        Assertions.assertFalse(paymentDictionaryListPage.isEntryIdPresent(entryId1),
                "Первая запись всё ещё существует после удаления (delete selected 2). EntryId=" + entryId1);
        Assertions.assertFalse(paymentDictionaryListPage.isEntryIdPresent(entryId2),
                "Вторая запись всё ещё существует после удаления (delete selected 2). EntryId=" + entryId2);
    }
}
