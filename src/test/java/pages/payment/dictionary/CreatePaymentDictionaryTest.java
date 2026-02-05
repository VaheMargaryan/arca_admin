package pages.payment.dictionary;

import baseUtils.api.ApiRequests;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pages.mainPage.MenuBarPage;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CreatePaymentDictionaryTest {

    private Playwright playwright;
    private Browser browser;

    private BrowserContext context;
    private Page page;

    private MenuBarPage menuBarPage;
    private PaymentDictionaryListPage paymentDictionaryListPage;
    private CreatePaymentDictionaryPage createPaymentDictionaryPage;

    private ApiRequests apiRequests;

    private static final String BASE_URL =
            System.getProperty("baseUrl",
                    System.getenv().getOrDefault("BASE_URL", "https://admin-web-dev.itguru.am/home"));

    private static final String API_BASE_URL =
            System.getProperty("apiBaseUrl",
                    System.getenv().getOrDefault("API_BASE_URL", "https://adminopenapi-dev.itguru.am"));

    // что открывается по Create
    private static final Pattern EXPECTED_FLOW_HEADERS = Pattern.compile("^(?:Create Payment Dictionary Items)$");

    // для cleanup
    private Integer createdDictionaryId = null;
    private String createdEntryId = null;

    @BeforeAll
    void beforeAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false)
        );
    }

    @AfterAll
    void afterAll() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void beforeEach() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1920, 1080));
        page = context.newPage();
        page.navigate(BASE_URL);

        menuBarPage = new MenuBarPage(page);
        paymentDictionaryListPage = new PaymentDictionaryListPage(page);
        createPaymentDictionaryPage = new CreatePaymentDictionaryPage(page);

        // API helper (заголовки максимально безопасные)
        apiRequests = new ApiRequests(context.request(), API_BASE_URL, buildApiHeaders());
        createdDictionaryId = null;
        createdEntryId = null;
    }

    @AfterEach
    void afterEach() {
        // Если ID ещё не определили — пробуем определить по entryId и всё равно удалить
        if (createdDictionaryId == null && createdEntryId != null) {
            try {
                goToPaymentDictionaryList();
                createdDictionaryId = paymentDictionaryListPage.getDictionaryIdByEntryId(createdEntryId);
            } catch (Exception ignored) {
                // если не нашли — уже ничего не сделаем через deleteDictionaries
            }
        }

        if (createdDictionaryId != null) {
            apiRequests.deleteDictionaries(List.of(createdDictionaryId));
        }

        if (context != null) context.close();
    }

    // ===== Хелперы =====

    private void goToPaymentDictionaryList() {
        menuBarPage.clickPaymentDictionary();
        assertThat(page).hasURL(Pattern.compile(".*/payment/dictionary/list.*"));
        paymentDictionaryListPage.waitOpened();
    }

    private void assertFlowOpenedAfterClick(String urlBefore) {
        Locator pageHeader = page.locator("app-page-title h3.page-title, h3.page-title").first();
        Locator dialogHeader = page.locator(
                "mat-dialog-container [mat-dialog-title], " +
                        "mat-dialog-container .mat-mdc-dialog-title, " +
                        "mat-dialog-container h1, mat-dialog-container h2, mat-dialog-container h3"
        ).first();

        long end = System.currentTimeMillis() + 5000;
        String lastSeenHeader = null;

        while (System.currentTimeMillis() < end) {
            if (!page.url().equals(urlBefore)) return;

            if (pageHeader.isVisible()) {
                try {
                    String text = pageHeader.innerText();
                    if (text != null) {
                        lastSeenHeader = text.trim();
                        if (EXPECTED_FLOW_HEADERS.matcher(lastSeenHeader).matches()) return;
                    }
                } catch (Exception ignored) {}
            }

            if (dialogHeader.isVisible()) {
                try {
                    String text = dialogHeader.innerText();
                    if (text != null) {
                        lastSeenHeader = text.trim();
                        if (EXPECTED_FLOW_HEADERS.matcher(lastSeenHeader).matches()) return;
                    }
                } catch (Exception ignored) {}
            }

            page.waitForTimeout(100);
        }

        Assertions.fail(
                "После клика не открылся ожидаемый flow: URL не изменился и заголовок не совпал.\n" +
                        "URL(before) = " + urlBefore + "\n" +
                        "URL(after)  = " + page.url() + "\n" +
                        "Last header = " + lastSeenHeader
        );
    }

    private void assertSuccessToast() {
        Locator successMsg = page.locator(
                        "mat-snack-bar-container, " +
                                "simple-snack-bar, " +
                                "[role='status'], " +
                                "[aria-live='polite']"
                )
                .filter(new Locator.FilterOptions().setHasText("The dictionary was created successfully."))
                .first();

        successMsg.waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE)
                .setTimeout(10_000));

        assertThat(successMsg).containsText("The dictionary was created successfully.");
    }

    private String randomEntryId() {
        // если поле строго numeric — делаем numeric
        int n = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.valueOf(n);
    }

    private String valueForLanguage(String language) {
        return switch (language) {
            case "Armenian" -> "պատահական տեքստի թեստ";
            case "English" -> "random text test";
            case "Russian" -> "рандомное значение тест";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private Map<String, String> buildApiHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
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

    // ===== Тесты =====
    @ParameterizedTest(name = "Language={0}")
    @ValueSource(strings = {"Armenian", "English", "Russian"})
    @DisplayName("Create Payment Dictionary Items -> создание через кнопку 'Create'")
    @Order(1)
    void createDictionary_fillFirstRow_smoke(String language) {
        goToPaymentDictionaryList();
        paymentDictionaryListPage.clickCreate();
        createPaymentDictionaryPage.waitOpened();

        String entryId = randomEntryId();
        String value = valueForLanguage(language);

        createdEntryId = entryId;

        createPaymentDictionaryPage.fillRow(
                0,
                "ErrorCode",
                language,
                entryId,
                value
        );

        createPaymentDictionaryPage.clickSave();

        assertSuccessToast();

        // Теперь получаем ID созданной записи из списка, чтобы удалить через API
        goToPaymentDictionaryList();
        createdDictionaryId = paymentDictionaryListPage.getDictionaryIdByEntryId(entryId);

        Assertions.assertNotNull(createdDictionaryId,
                "Не смогли найти созданный dictionary item в списке по Entry ID=" + entryId);
    }
}
