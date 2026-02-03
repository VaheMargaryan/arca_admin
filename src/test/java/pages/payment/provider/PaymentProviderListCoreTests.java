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

        // ждём, что роутинг реально привёл на Provider List
        assertThat(page).hasURL(Pattern.compile(".*/payment/provider/list.*"));
    }

    /**
     * Универсальная проверка: после клика должно либо измениться URL, либо открыться диалог/панель.
     * (так как точный expected UI ты не описал)
     */
    private void assertFlowOpenedAfterClick(String urlBefore) throws InterruptedException {
        Locator anyDialog = page.locator("mat-dialog-container, [role='dialog']");
        Locator anyOverlayPane = page.locator(".cdk-overlay-pane:visible");
        Locator anyDrawer = page.locator("mat-drawer:visible, mat-sidenav:visible, .mat-drawer:visible, .mat-sidenav:visible");

        long end = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < end) {
            if (!page.url().equals(urlBefore)) return;
            if (anyDialog.first().isVisible()) return;
            if (anyDrawer.first().isVisible()) return;
            if (anyOverlayPane.first().isVisible()) return;

            page.waitForTimeout(100);
        }

        Assertions.fail("После клика не открылся ни диалог/панель и URL не изменился.");
    }

    private void assertHeaderText(String expectedHeader) {
        // Заголовок страницы (по твоему DOM это h3.page-title внутри app-page-title)
        Locator header = page.locator("app-page-title h3.page-title").first();

        // Playwright Assertions сами ждут появления/изменения текста (auto-wait)
        assertThat(header).hasText(expectedHeader);
    }

    // ===== Тесты =====

    @Test
    @DisplayName("Страница открылась и не пустая")
    @Order(1)
    void providerList_shouldLoad_and_providerNameColumnNotEmpty() {
        goToPaymentProviderList();

        List<String> providerNames = providerListPage.columnTexts("providerName");
        Assertions.assertFalse(providerNames.isEmpty(), "Колонка providerName пустая (данные не загрузились или локатор неверный).");

        System.out.println("First providerName = " + providerNames.get(0));
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Create Payment Provider'")
    @Order(2)
    void createButton_shouldOpenCreateFlow() throws InterruptedException {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickCreateButton();

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit'")
    @Order(3)
    void actionsMenu_edit_shouldOpenFlow_forFirstRow() throws InterruptedException {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEdit(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit external connections'")
    @Order(4)
    void actionsMenu_editExternalConnections_shouldOpenFlow_forFirstRow() throws InterruptedException {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEditExternalConnections(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit accounts'")
    @Order(5)
    void actionsMenu_editAccounts_shouldOpenFlow_forFirstRow() throws InterruptedException {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEditAccounts(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit service identifiers'")
    @Order(6)
    void actionsMenu_editServiceIdentifiers_shouldOpenFlow_forFirstRow() throws InterruptedException {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEditServiceIdentifiers(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit certificate'")
    @Order(7)
    void actionsMenu_editCertificate_shouldOpenFlow_forFirstRow() throws InterruptedException {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEditCertificate(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия страницы 'Edit currencies'")
    @Order(8)
    void actionsMenu_editCurrencies_shouldOpenFlow_forFirstRow() throws InterruptedException {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickEditCurrencies(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия 'Details'")
    @Order(9)
    void detailsButton_shouldOpenFlow_forFirstRow() throws InterruptedException {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickDetails(0);

        assertFlowOpenedAfterClick(urlBefore);
    }

    @Test
    @DisplayName("Payment Provider List -> проверка открытия 'Add certificate'")
    @Order(10)
    void addCertificateButton_shouldOpenFlow_forFirstRow() throws InterruptedException {
        goToPaymentProviderList();

        String urlBefore = page.url();
        providerListPage.clickAddCertificate(0);

        assertFlowOpenedAfterClick(urlBefore);
    }
}
