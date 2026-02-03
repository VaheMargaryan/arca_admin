package pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.*;
import pages.mainPage.MenuBarPage;
import pages.payment.provider.PaymentProviderListPage;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class TestClass {
    static Playwright playwright;
    static Browser browser;
    static BrowserContext context;
    static Page page;
    private MenuBarPage menuBarPage;
    private PaymentProviderListPage paymentProviderListPage;

    // Подставь свой URL (можно передавать: -DbaseUrl=http://... )
    private static final String BASE_URL =
            System.getProperty("baseUrl",
                    System.getenv().getOrDefault("BASE_URL", "https://admin-web-dev.itguru.am/home"));

    private static final int WIDTH = Integer.parseInt(System.getProperty("width", "1920"));
    private static final int HEIGHT = Integer.parseInt(System.getProperty("height", "1080"));

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

//    @AfterAll
//    static void closeBrowser() {
//        playwright.close();
//    }

    @BeforeEach
    void beforeEach() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(WIDTH, HEIGHT)
        );

        page = context.newPage();
        page.navigate(BASE_URL);

        menuBarPage = new MenuBarPage(page);
        paymentProviderListPage = new PaymentProviderListPage(page);
    }

//    @AfterEach
//    void closeContext() {
//        context.close();
//    }

    @Test
    public void checkOpenPaymentBlock() throws InterruptedException {
        menuBarPage.clickPaymentProvider();

        // 1) дождаться роутинга
         assertThat(page).hasURL(Pattern.compile(".*/payment/provider/list.*"));

        List<String> providerNames = paymentProviderListPage.columnTexts("providerName");
        System.out.println(providerNames.get(0));
    }

}
