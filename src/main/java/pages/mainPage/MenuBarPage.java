package pages.mainPage;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

public class MenuBarPage {
    private final Page page;

    // ===== Локаторы =====
    private final Locator burgerButton;

    // Признак состояния меню (по скрину у aside есть класс collapsed)
    // Если collapsed нет -> меню раскрыто
    private final Locator sidebarCollapsed;
    private final Locator sidebar;

    // Payment
    private final Locator paymentBlock;
    private final Locator tabPaymentProvider;
    private final Locator tabPaymentMerchant;
    private final Locator tabPaymentDictionary;

    // Card Inspection
    private final Locator cardInspectionBlock;
    private final Locator tabCardInspectionProvider;
    private final Locator tabCardInspectionMerchant;
    private final Locator tabCardInspectionDictionary;

    // User Management
    private final Locator userManagementBlock;
    private final Locator tabUMUsers;
    private final Locator tabUMUsersRoles;
    private final Locator tabUMDictionary;

    // Language
    private final Locator languageBar;
    private final Locator armLanguage;
    private final Locator engLanguage;
    private final Locator rusLanguage;

    public MenuBarPage(Page page) {
        this.page = page;

        this.sidebar = page.locator("aside.sidebar-nav");
        this.sidebarCollapsed = page.locator("aside.sidebar-nav.collapsed");

        this.burgerButton = page.locator("aside.sidebar-nav button:has(mat-icon:has-text('menu'))");

        this.paymentBlock = page.locator("aside.sidebar-nav a[aria-label='global.payment']");
        this.tabPaymentProvider = page.locator("aside.sidebar-nav a[href='/payment/provider/list']");
        this.tabPaymentMerchant = page.locator("aside.sidebar-nav a[href='/payment/merchant/list']");
        this.tabPaymentDictionary = page.locator("aside.sidebar-nav a[href='/payment/dictionary/list']");

        this.cardInspectionBlock = page.locator("aside.sidebar-nav a[aria-label='global.card_inspection']");
        this.tabCardInspectionProvider = page.locator("aside.sidebar-nav a[href='/card-inspection/provider/list']");
        this.tabCardInspectionMerchant = page.locator("aside.sidebar-nav a[href='/card-inspection/merchant/list']");
        this.tabCardInspectionDictionary = page.locator("aside.sidebar-nav a[href='/card-inspection/dictionary/list']");

        this.userManagementBlock = page.locator("aside.sidebar-nav a[aria-label='global.user_management']");
        this.tabUMUsers = page.locator("aside.sidebar-nav a[href='/user-management/users/list']");
        this.tabUMUsersRoles = page.locator("aside.sidebar-nav a[href='/user-management/user-roles/list']");
        this.tabUMDictionary = page.locator("aside.sidebar-nav a[href='/user-management/dictionary/list']");

        this.languageBar = page.locator("app-language-switcher .mat-mdc-select-trigger");

        // ВАЖНО: mat-option-0/1/2 хрупко, но оставляю как у тебя.
        this.armLanguage = page.locator("#mat-option-0");
        this.engLanguage = page.locator("#mat-option-1");
        this.rusLanguage = page.locator("#mat-option-2");
    }

    // ===== Приватные хелперы =====

    /** Кликает бургер только если меню свернуто (toggle-safe). */
    private void ensureMenuExpanded() {
        if (sidebarCollapsed.count() > 0) {
            burgerButton.click();
        }
    }

    /** Универсальный клик: открыть меню -> открыть раздел -> кликнуть вкладку */
    private void clickMenuItem(Locator section, Locator item) {
        ensureMenuExpanded();
        section.click();
        item.click();
    }

    // ===== Общие =====

    @Step("Ensure burger menu expanded")
    public void openBurgerMenu() {
        ensureMenuExpanded();
    }

    // ===== Payment =====
    @Step("Open Payment section")
    public void clickPaymentBlock() {
        ensureMenuExpanded();
        paymentBlock.click();
    }

    @Step("Payment -> Provider")
    public void clickPaymentProvider() {
        clickMenuItem(paymentBlock, tabPaymentProvider);
    }

    @Step("Payment -> Merchant")
    public void clickPaymentMerchant() {
        clickMenuItem(paymentBlock, tabPaymentMerchant);
    }

    @Step("Payment -> Dictionary")
    public void clickPaymentDictionary() {
        clickMenuItem(paymentBlock, tabPaymentDictionary);
    }

    // ===== Card Inspection =====
    @Step("Open Card Inspection section")
    public void clickCardInspectionBlock() {
        ensureMenuExpanded();
        cardInspectionBlock.click();
    }

    @Step("Card Inspection -> Provider")
    public void clickCardInspectionProvider() {
        clickMenuItem(cardInspectionBlock, tabCardInspectionProvider);
    }

    @Step("Card Inspection -> Merchant")
    public void clickCardInspectionMerchant() {
        clickMenuItem(cardInspectionBlock, tabCardInspectionMerchant);
    }

    @Step("Card Inspection -> Dictionary")
    public void clickCardInspectionDictionary() {
        clickMenuItem(cardInspectionBlock, tabCardInspectionDictionary);
    }

    // ===== User Management =====
    @Step("Open User Management section")
    public void clickUserManagementBlock() {
        ensureMenuExpanded();
        userManagementBlock.click();
    }

    @Step("User Management -> Users")
    public void clickUMUsers() {
        clickMenuItem(userManagementBlock, tabUMUsers);
    }

    @Step("User Management -> User Roles")
    public void clickUMUsersRoles() {
        clickMenuItem(userManagementBlock, tabUMUsersRoles);
    }

    @Step("User Management -> Dictionary")
    public void clickUMDictionary() {
        clickMenuItem(userManagementBlock, tabUMDictionary);
    }

    // ===== Language =====

    @Step("Open language dropdown")
    public void clickLanguageBar() {
        languageBar.click();
    }

    @Step("Select Armenian language")
    public void clickArmLanguage() {
        clickLanguageBar();
        armLanguage.click();
    }

    @Step("Select English language")
    public void clickEngLanguage() {
        clickLanguageBar();
        engLanguage.click();
    }

    @Step("Select Russian language")
    public void clickRusLanguage() {
        clickLanguageBar();
        rusLanguage.click();
    }
}
