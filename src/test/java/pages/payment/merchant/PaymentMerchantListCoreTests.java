package pages.payment.merchant;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import pages.mainPage.MenuBarPage;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaymentMerchantListCoreTests {

    private Playwright playwright;
    private Browser browser;

    private BrowserContext context;
    private Page page;

    private MenuBarPage menuBarPage;
    private PaymentMerchantListPage merchantListPage;

    private static final String BASE_URL =
            System.getProperty("baseUrl",
                    System.getenv().getOrDefault("BASE_URL", "https://admin-web-dev.itguru.am/home"));

    // ===== Ожидаемые заголовки flow (точные значения) =====
    private static final Pattern EXPECTED_FLOW_HEADERS = Pattern.compile(
            "^(?:"
                    + "Create Payment Merchant"
                    + "Merchant details"
                    + "|Add merchant certificate"
                    + "|Edit Payment Merchant"
                    + "|Edit Payment Merchant External Connections"
                    + "|Edit Payment Merchant Service Identifiers"
                    + "|Edit Payment Merchant Certificate"
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
        merchantListPage = new PaymentMerchantListPage(page);
    }

    @AfterEach
    void afterEach() {
        if (context != null) context.close();
    }

    // ===== Хелперы =====

    private void goToPaymentMerchantList() {
        // Должен быть метод в MenuBarPage
        menuBarPage.clickPaymentMerchant();

        // Ждём, что роутинг реально привёл на Merchant List
        assertThat(page).hasURL(Pattern.compile(".*/payment/merchant/list.*"));
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
                    // DOM мог перерисоваться в момент чтения — пробуем дальше
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
                    // DOM мог перерисоваться — пробуем дальше
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

    // ===== Тесты =====

    @Test
    @DisplayName("Страница открылась и не пустая")
    @Order(1)
    void merchantList_shouldLoad_and_merchantNameColumnNotEmpty() {
        goToPaymentMerchantList();

        List<String> merchantNames = merchantListPage.columnTexts("merchantName");
        Assertions.assertFalse(merchantNames.isEmpty(),
                "Колонка merchantName пустая (данные не загрузились или локатор неверный).");

        System.out.println("First merchantName = " + merchantNames.get(0));
    }

    @Test
    @DisplayName("Payment Merchant List -> проверка открытия страницы 'Create'")
    @Order(2)
    void createButton_shouldOpenCreateFlow() {
        goToPaymentMerchantList();

        String urlBefore = page.url();
        merchantListPage.clickCreate();

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Merchant List -> проверка открытия страницы 'Edit Payment Merchant'")
    @Order(3)
    void actionsMenu_edit_shouldOpenFlow_forFirstRow() {
        goToPaymentMerchantList();

        String urlBefore = page.url();
        merchantListPage.clickEdit(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Merchant List -> проверка открытия 'Edit Payment Merchant External Connections'")
    @Order(4)
    void actionsMenu_editExternalConnections_shouldOpenFlow_forFirstRow() {
        goToPaymentMerchantList();

        String urlBefore = page.url();
        merchantListPage.clickEditExternalConnections(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Merchant List -> проверка открытия 'Edit Payment Merchant Service Identifiers'")
    @Order(5)
    void actionsMenu_editServiceIdentifiers_shouldOpenFlow_forFirstRow() {
        goToPaymentMerchantList();

        String urlBefore = page.url();
        merchantListPage.clickEditServiceIdentifiers(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Merchant List -> проверка открытия 'Edit Payment Merchant Certificate'")
    @Order(6)
    void actionsMenu_editCertificate_shouldOpenFlow_forFirstRow() {
        goToPaymentMerchantList();

        String urlBefore = page.url();
        merchantListPage.clickEditCertificate(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Merchant List -> проверка открытия 'Merchant details'")
    @Order(7)
    void detailsButton_shouldOpenFlow_forFirstRow() {
        goToPaymentMerchantList();

        String urlBefore = page.url();
        merchantListPage.clickDetails(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Merchant List -> проверка открытия 'Add merchant certificate'")
    @Order(8)
    void addCertificateButton_shouldOpenFlow_forFirstRow() {
        goToPaymentMerchantList();

        String urlBefore = page.url();
        merchantListPage.clickAddCertificate(0);

        assertFlowOpenedAfterClick(urlBefore);
    }
}
