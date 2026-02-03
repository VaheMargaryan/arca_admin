package pages.payment.provider;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import pages.mainPage.MenuBarPage;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaymentProviderListCoreTests {

    private Playwright playwright;
    private Browser browser;

    private BrowserContext context;
    private Page page;

    private MenuBarPage menuBarPage;
    private PaymentProviderListPage providerListPage;

    private static final String BASE_URL =
            System.getProperty("baseUrl",
                    System.getenv().getOrDefault("BASE_URL", "https://admin-web-dev.itguru.am/home"));

    // ===== Ожидаемые заголовки flow (точные значения) =====
    private static final Pattern EXPECTED_FLOW_HEADERS = Pattern.compile(
            "^(?:"
                    + "Edit Payment Provider"
                    + "|Edit Payment Provider External Connections"
                    + "|Edit Payment Provider Accounts"
                    + "|Edit Payment Provider Service Identifiers"
                    + "|Edit Payment Provider Certificate"
                    + "|Edit Payment Provider Currencies"
                    + "|Add provider certificate"
                    + "|Provider details"
                    + "|Create Payment Provider"
                    + ")$"
    );

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
    void beforeEach() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1920, 1080));
        page = context.newPage();

        page.navigate(BASE_URL);

        menuBarPage = new MenuBarPage(page);
        providerListPage = new PaymentProviderListPage(page);
    }

    @AfterEach
    void afterEach() {
        if (context != null) context.close();
    }

    // ===== Хелперы =====

    private void goToPaymentProviderList() {
        menuBarPage.clickPaymentProvider();

        // Ждём, что роутинг реально привёл на Provider List
        assertThat(page).hasURL(Pattern.compile(".*/payment/provider/list.*"));
    }

    /**
     * Универсальная проверка: после клика должно либо измениться URL,
     * либо появиться заголовок (страницы/диалога) с одним из ожидаемых текстов.
     */
    private void assertFlowOpenedAfterClick(String urlBefore) {
        Locator pageHeader = page.locator("app-page-title h3.page-title").first();

        Locator dialogHeader = page.locator(
                "mat-dialog-container [mat-dialog-title], " +
                        "mat-dialog-container .mat-mdc-dialog-title, " +
                        "mat-dialog-container h1, mat-dialog-container h2, mat-dialog-container h3"
        ).first();

        long end = System.currentTimeMillis() + 5000;
        String lastSeenHeader = null;

        while (System.currentTimeMillis() < end) {

            // 1) URL изменился — значит открылся новый роут/экран
            if (!page.url().equals(urlBefore)) return;

            // 2) Проверяем заголовок страницы
            if (pageHeader.isVisible()) {
                try {
                    String text = pageHeader.innerText();
                    if (text != null) {
                        lastSeenHeader = text.trim();
                        if (EXPECTED_FLOW_HEADERS.matcher(lastSeenHeader).matches()) return;
                    }
                } catch (Exception ignored) {
                    // элемент мог перерисоваться в момент чтения — просто пробуем дальше
                }
            }

            // 3) Проверяем заголовок диалога
            if (dialogHeader.isVisible()) {
                try {
                    String text = dialogHeader.innerText();
                    if (text != null) {
                        lastSeenHeader = text.trim();
                        if (EXPECTED_FLOW_HEADERS.matcher(lastSeenHeader).matches()) return;
                    }
                } catch (Exception ignored) {
                    // элемент мог перерисоваться в момент чтения — просто пробуем дальше
                }
            }

            // Короткое ожидание и повтор
            page.waitForTimeout(100);
        }

        Assertions.fail(
                "После клика не открылся ожидаемый flow: URL не изменился и заголовок не совпал с ожидаемыми.\n" +
                        "URL(before) = " + urlBefore + "\n" +
                        "URL(after)  = " + page.url() + "\n" +
                        "Last header = " + lastSeenHeader
        );
    }

    // Оставил метод, но сейчас тесты используют assertFlowOpenedAfterClick
    private void assertHeaderText(String expectedHeader) {
        Locator header = page.locator("app-page-title h3.page-title").first();
        assertThat(header).hasText(expectedHeader);
    }

    // ===== Тесты =====

    @Test
    @DisplayName("Страница открылась и не пустая")
    @Order(1)
    void providerList_shouldLoad_and_providerNameColumnNotEmpty() {
        goToPaymentProviderList();

        List<String> providerNames = providerListPage.columnTexts("providerName");
        Assertions.assertFalse(providerNames.isEmpty(),
                "Колонка providerName пустая (данные не загрузились или локатор неверный).");

        System.out.println("First providerName = " + providerNames.get(0));
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Create Payment Provider'")
    @Order(2)
    void createButton_shouldOpenCreateFlow() {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickCreateButton();

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit'")
    @Order(3)
    void actionsMenu_edit_shouldOpenFlow_forFirstRow() {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEdit(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit external connections'")
    @Order(4)
    void actionsMenu_editExternalConnections_shouldOpenFlow_forFirstRow() {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEditExternalConnections(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit accounts'")
    @Order(5)
    void actionsMenu_editAccounts_shouldOpenFlow_forFirstRow() {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEditAccounts(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit service identifiers'")
    @Order(6)
    void actionsMenu_editServiceIdentifiers_shouldOpenFlow_forFirstRow() {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEditServiceIdentifiers(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit certificate'")
    @Order(7)
    void actionsMenu_editCertificate_shouldOpenFlow_forFirstRow() {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEditCertificate(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit currencies'")
    @Order(8)
    void actionsMenu_editCurrencies_shouldOpenFlow_forFirstRow() {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEditCurrencies(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия 'Details'")
    @Order(9)
    void detailsButton_shouldOpenFlow_forFirstRow() {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickDetails(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия 'Add certificate'")
    @Order(10)
    void addCertificateButton_shouldOpenFlow_forFirstRow() {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickAddCertificate(0);

        assertFlowOpenedAfterClick(urlBefore);
    }
}
