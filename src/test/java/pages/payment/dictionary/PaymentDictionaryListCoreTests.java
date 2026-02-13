package pages.payment.dictionary;

import baseUtils.api.ApiRequests;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pages.mainPage.MenuBarPage;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
    private EditPaymentDictionaryItemsPage editPaymentDictionaryItemsPage;

    private ApiRequests apiRequests;

    private static final String BASE_URL =
            System.getProperty("baseUrl",
                    System.getenv().getOrDefault("BASE_URL", "https://admin-web-dev.itguru.am/home"));

    private static final String API_BASE_URL =
            System.getProperty("apiBaseUrl",
                    System.getenv().getOrDefault("API_BASE_URL", "https://adminopenapi-dev.itguru.am"));

    private static final String TOAST_DELETED = "The dictionary was deleted successfully.";

    // URL patterns
    private static final Pattern URL_LIST = Pattern.compile(".*/payment/dictionary/list.*");
    private static final Pattern URL_EDIT = Pattern.compile(".*/payment/dictionary/edit.*");

    // По твоему payload на скрине: English = langId 2
    private static final int LANG_EN = 2;
    private static final int BEHAVIOR_DEFAULT = 1;

    // ===== Test data created in @BeforeEach =====
    // Здесь всегда "актуальные" entryId (после edit — обновляем список)
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
        editPaymentDictionaryItemsPage = new EditPaymentDictionaryItemsPage(page);

        apiRequests = new ApiRequests(context.request(), API_BASE_URL, buildApiHeaders());

        createdEntryIds.clear();

        String methodName = testInfo.getTestMethod().map(Method::getName).orElse("");

        int needToCreate = switch (methodName) {
            case "deleteViaTrashIcon_shouldDeleteWithConfirmModal" -> 1;
            case "deleteSelected_one_shouldDeleteWithoutModal" -> 1;
            case "deleteSelected_two_shouldDeleteWithoutModal" -> 2;
            case "editSelected_one_shouldUpdateFields" -> 1;
            case "editSelected_two_shouldUpdateFields" -> 2;
            default -> 0;
        };

        for (int i = 1; i <= needToCreate; i++) {
            long entryId = randomEntryIdLong();
            String entryIdStr = String.valueOf(entryId);

            apiRequests.addDictionaryItem(
                    "ProviderType",
                    entryId,
                    LANG_EN,
                    "autotest data for " + methodName + " #" + i,
                    BEHAVIOR_DEFAULT
            );

            createdEntryIds.add(entryIdStr);
        }

        // убедимся, что данные реально появились в UI (иначе смысл delete/edit теряется)
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
        // Safety cleanup: если тест упал и что-то осталось — удалим через API
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
        assertThat(page).hasURL(URL_LIST);
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

    // ===== DELETE TESTS =====

    @Test
    @Order(1)
    @DisplayName("Payment Dictionary List -> delete via trash icon")
    void deleteViaTrashIcon_shouldDeleteWithConfirmModal() {
        Assertions.assertEquals(1, createdEntryIds.size(), "Ожидали 1 тестовую запись");
        String entryId = createdEntryIds.get(0);

        goToPaymentDictionaryList();
        paymentDictionaryListPage.waitEntryIdVisible(entryId);

        paymentDictionaryListPage.clickTrashDeleteByEntryId(entryId);
        paymentDictionaryListPage.confirmDeleteModal();

        assertToast(TOAST_DELETED);

        // проверяем, что удалили именно созданный entryId
        paymentDictionaryListPage.waitEntryIdDisappears(entryId);
        Assertions.assertFalse(paymentDictionaryListPage.isEntryIdPresent(entryId),
                "Запись всё ещё существует после удаления (trash). EntryId=" + entryId);

        // чтобы cleanup не искал уже удалённое
        createdEntryIds.clear();
    }

    @Test
    @Order(2)
    @DisplayName("Payment Dictionary List -> delete selected (1 checkbox)")
    void deleteSelected_one_shouldDeleteWithoutModal() {
        Assertions.assertEquals(1, createdEntryIds.size(), "Ожидали 1 тестовую запись");
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

        goToPaymentDictionaryList();
        paymentDictionaryListPage.waitEntryIdDisappears(entryId);
        Assertions.assertFalse(paymentDictionaryListPage.isEntryIdPresent(entryId),
                "Запись всё ещё существует после удаления (delete selected 1). EntryId=" + entryId);

        createdEntryIds.clear();
    }

    @Test
    @Order(3)
    @DisplayName("Payment Dictionary List -> delete selected (2 checkboxes)")
    void deleteSelected_two_shouldDeleteWithoutModal() {
        Assertions.assertEquals(2, createdEntryIds.size(), "Ожидали 2 тестовые записи");
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

        goToPaymentDictionaryList();
        paymentDictionaryListPage.waitEntryIdDisappears(entryId1);
        paymentDictionaryListPage.waitEntryIdDisappears(entryId2);

        Assertions.assertFalse(paymentDictionaryListPage.isEntryIdPresent(entryId1),
                "Первая запись всё ещё существует. EntryId=" + entryId1);
        Assertions.assertFalse(paymentDictionaryListPage.isEntryIdPresent(entryId2),
                "Вторая запись всё ещё существует. EntryId=" + entryId2);

        createdEntryIds.clear();
    }

    // ===== EDIT TEST DATA =====

    static Stream<org.junit.jupiter.params.provider.Arguments> editOneCases() {
        return Stream.of(
                arguments("Armenian", 11L, "պատահական տեքստի թեստ"),
                arguments("English",  22L, "random text test"),
                arguments("Russian",  33L, "рандомное значение тест")
        );
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> editTwoCases() {
        return Stream.of(
                arguments("English",  101L, "random text test first", 202L, "random text test second"),
                arguments("Russian",  303L, "рандомное значение тест первое", 404L, "рандомное значение тест второе")
        );
    }

    // ===== EDIT TESTS =====

    @ParameterizedTest(name = "Edit one: lang={0}, idSeed={1}, value={2}")
    @MethodSource("editOneCases")
    @Order(4)
    @DisplayName("Payment Dictionary List -> edit selected (1 checkbox) update language + entryId + value")
    void editSelected_one_shouldUpdateFields(String newLanguage, long idSeed, String newValue) {
        Assertions.assertEquals(1, createdEntryIds.size(), "Ожидали 1 тестовую запись");
        String oldEntryId = createdEntryIds.get(0);

        long base = randomEntryIdLong();
        String newEntryId = String.valueOf(base + idSeed);

        goToPaymentDictionaryList();
        paymentDictionaryListPage.waitEntryIdVisible(oldEntryId);

        paymentDictionaryListPage.selectRowByEntryId(oldEntryId);
        paymentDictionaryListPage.clickEditSelected();

        assertThat(page).hasURL(URL_EDIT);

        editPaymentDictionaryItemsPage.waitOpened();

        editPaymentDictionaryItemsPage.setRowLanguage(0, newLanguage);
        editPaymentDictionaryItemsPage.setRowEntryId(0, newEntryId);
        editPaymentDictionaryItemsPage.setRowValue(0, newValue);

        editPaymentDictionaryItemsPage.clickSaveAndWaitList(URL_LIST);

        // 1) в списке старого id нет
        goToPaymentDictionaryList();
        paymentDictionaryListPage.waitEntryIdDisappears(oldEntryId);

        // 2) новый id появился
        paymentDictionaryListPage.waitEntryIdVisible(newEntryId);
        Assertions.assertTrue(paymentDictionaryListPage.isEntryIdPresent(newEntryId),
                "Не нашли обновлённую запись в списке. EntryId=" + newEntryId);

        // 3) доп.проверка: переоткрыть Edit и убедиться, что язык/id/value сохранились
        paymentDictionaryListPage.selectRowByEntryId(newEntryId);
        paymentDictionaryListPage.clickEditSelected();
        editPaymentDictionaryItemsPage.waitOpened();

        Assertions.assertEquals(newEntryId, editPaymentDictionaryItemsPage.getRowEntryId(0), "EntryId не совпал после сохранения");
        Assertions.assertTrue(editPaymentDictionaryItemsPage.getRowValue(0).contains(newValue), "Value не совпал после сохранения");
        Assertions.assertTrue(editPaymentDictionaryItemsPage.getRowLanguage(0).contains(newLanguage), "Language не совпал после сохранения");

        // важно для cleanup: теперь актуальный entryId = новый
        createdEntryIds.clear();
        createdEntryIds.add(newEntryId);
    }

    @ParameterizedTest(name = "Edit two: lang={0}, id1Seed={1}, v1={2}, id2Seed={3}, v2={4}")
    @MethodSource("editTwoCases")
    @Order(5)
    @DisplayName("Payment Dictionary List -> edit selected (2 checkboxes) update language + entryId + value")
    void editSelected_two_shouldUpdateFields(String newLanguage,
                                             long id1Seed, String newValue1,
                                             long id2Seed, String newValue2) {
        Assertions.assertEquals(2, createdEntryIds.size(), "Ожидали 2 тестовые записи");
        String oldEntryId1 = createdEntryIds.get(0);
        String oldEntryId2 = createdEntryIds.get(1);

        long base = randomEntryIdLong();
        String newEntryId1 = String.valueOf(base + id1Seed);
        String newEntryId2 = String.valueOf(base + id2Seed);

        goToPaymentDictionaryList();
        paymentDictionaryListPage.waitEntryIdVisible(oldEntryId1);
        paymentDictionaryListPage.waitEntryIdVisible(oldEntryId2);

        paymentDictionaryListPage.selectRowByEntryId(oldEntryId1);
        paymentDictionaryListPage.selectRowByEntryId(oldEntryId2);
        paymentDictionaryListPage.clickEditSelected();

        assertThat(page).hasURL(URL_EDIT);

        editPaymentDictionaryItemsPage.waitOpened();
        Assertions.assertEquals(2, editPaymentDictionaryItemsPage.rowsCount(), "Ожидали 2 строки на edit");

        // row 0
        editPaymentDictionaryItemsPage.setRowLanguage(0, newLanguage);
        editPaymentDictionaryItemsPage.setRowEntryId(0, newEntryId1);
        editPaymentDictionaryItemsPage.setRowValue(0, newValue1);

        // row 1
        editPaymentDictionaryItemsPage.setRowLanguage(1, newLanguage);
        editPaymentDictionaryItemsPage.setRowEntryId(1, newEntryId2);
        editPaymentDictionaryItemsPage.setRowValue(1, newValue2);

        editPaymentDictionaryItemsPage.clickSaveAndWaitList(URL_LIST);

        goToPaymentDictionaryList();

        // старых нет
        paymentDictionaryListPage.waitEntryIdDisappears(oldEntryId1);
        paymentDictionaryListPage.waitEntryIdDisappears(oldEntryId2);

        // новых два появились
        paymentDictionaryListPage.waitEntryIdVisible(newEntryId1);
        paymentDictionaryListPage.waitEntryIdVisible(newEntryId2);

        Assertions.assertTrue(paymentDictionaryListPage.isEntryIdPresent(newEntryId1), "Нет записи newEntryId1=" + newEntryId1);
        Assertions.assertTrue(paymentDictionaryListPage.isEntryIdPresent(newEntryId2), "Нет записи newEntryId2=" + newEntryId2);

        // update для cleanup
        createdEntryIds.clear();
        createdEntryIds.add(newEntryId1);
        createdEntryIds.add(newEntryId2);
    }
}
