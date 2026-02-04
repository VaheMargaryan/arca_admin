package pages.payment.merchant;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;

import java.util.List;

public class PaymentMerchantListPage {
    private final Page page;

    // ===== Локаторы страницы =====

    // Заголовок страницы (на скрине: "Payment Merchant List")
    private final Locator pageTitle;

    // Таблица со списком (mat-table) — работаем через role='table'
    private final Locator table;

    // Кнопка Create (в правом верхнем углу)
    private final Locator createButton;

    // ===== Конструктор =====

    public PaymentMerchantListPage(Page page) {
        this.page = page;

        this.pageTitle = page.locator("app-page-title h3.page-title, h3.page-title").first();

        // Таблица на странице
        this.table = page.locator("main table[role='table'], table[role='table']").first();

        this.createButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Create").setExact(true)
        );
    }

    // ===== Таблица (универсально по mat-column-<key>) =====

    /**
     * Строки таблицы (tbody).
     * Angular Material: tr.mat-mdc-row или tr[role='row'].
     */
    private Locator rows() {
        return table.locator("tbody tr.mat-mdc-row, tbody tr[role='row'], tbody tr");
    }

    /**
     * Строка по индексу (0-based).
     */
    public Locator rowByIndex(int rowIndex) {
        return rows().nth(rowIndex);
    }

    /**
     * Ячейки колонки по ключу.
     * columnKey — это часть после "mat-column-" в DevTools.
     * Пример: "merchantName", "providerId", "operationRole" и т.д.
     */
    public Locator columnCells(String columnKey) {
        return table.locator("tbody tr td.mat-column-" + columnKey);
    }

    /**
     * Тексты из колонки.
     * Перед чтением ждём, что первая ячейка стала видимой => данные подгрузились.
     */
    public List<String> columnTexts(String columnKey) {
        columnCells(columnKey)
                .first()
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        return columnCells(columnKey)
                .allTextContents()
                .stream()
                .map(String::trim)
                .toList();
    }

    // ===== Actions (иконки + меню) =====

    /**
     * Ячейка Actions в строке (обычно mat-column-actions / cdk-column-actions).
     */
    private Locator actionsCell(Locator row) {
        return row.locator("td.mat-column-actions, td.cdk-column-actions").first();
    }

    /**
     * Кнопка Details в Actions (иконка info).
     * На скрине: <mat-icon aria-label="global.actions.details" ...>
     */
    private Locator detailsButton(Locator row) {
        return actionsCell(row)
                .locator("button:has(mat-icon[aria-label='global.actions.details'])")
                .first();
    }

    /**
     * Кнопка Add certificate в Actions (иконка add).
     * На скрине: <mat-icon aria-label="payment_merchant.table.add_certificate" ...>
     */
    private Locator addCertificateButton(Locator row) {
        return actionsCell(row)
                .locator("button:has(mat-icon[aria-label='payment.merchant.table.add_certificate'])")
                .first();
    }

    /**
     * Кнопка "три точки" (menu trigger) внутри Actions.
     */
    private Locator kebabMenuButton(Locator row) {
        return actionsCell(row)
                .locator("button.mat-mdc-menu-trigger, button[aria-haspopup='menu']")
                .first();
    }

    /**
     * Видимая панель открытого меню (Angular Material menu).
     */
    private Locator visibleMenuPanel() {
        return page.locator("div.mat-mdc-menu-panel:visible").first();
    }

    /**
     * Пункт меню по точному имени (иначе будет strict mode violation).
     * Пример: "Edit", "Edit external connections", ...
     */
    private Locator menuItemByNameExact(String name) {
        return visibleMenuPanel().getByRole(
                AriaRole.MENUITEM,
                new Locator.GetByRoleOptions().setName(name).setExact(true)
        );
    }

    /**
     * Открыть actions-меню для строки и дождаться, что меню реально показалось.
     */
    @Step("Open actions menu for row #{rowIndex}")
    public void openActionsMenu(int rowIndex) {
        Locator row = rowByIndex(rowIndex);

        kebabMenuButton(row).click();

        // ждём появление меню
        visibleMenuPanel().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    // ===== Клики по кнопкам/пунктам =====

    @Step("Click Create button")
    public void clickCreate() {
        createButton.click();
    }

    /**
     * Actions -> Details (иконка info)
     */
    @Step("Actions -> Details (row #{rowIndex})")
    public void clickDetails(int rowIndex) {
        Locator row = rowByIndex(rowIndex);

        // на всякий случай: ждём, что строка/Actions реально видимы
        actionsCell(row).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        detailsButton(row).click();
    }

    /**
     * Actions -> Add certificate (иконка add)
     */
    @Step("Actions -> Add certificate (row #{rowIndex})")
    public void clickAddCertificate(int rowIndex) {
        Locator row = rowByIndex(rowIndex);

        actionsCell(row).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        addCertificateButton(row).click();
    }

    // ===== Actions меню (kebab) =====

    @Step("Actions -> Edit (row #{rowIndex})")
    public void clickEdit(int rowIndex) {
        openActionsMenu(rowIndex);
        menuItemByNameExact("Edit").click();
    }

    @Step("Actions -> Edit external connections (row #{rowIndex})")
    public void clickEditExternalConnections(int rowIndex) {
        openActionsMenu(rowIndex);
        menuItemByNameExact("Edit external connections").click();
    }

    @Step("Actions -> Edit service identifiers (row #{rowIndex})")
    public void clickEditServiceIdentifiers(int rowIndex) {
        openActionsMenu(rowIndex);
        menuItemByNameExact("Edit service identifiers").click();
    }

    @Step("Actions -> Edit certificate (row #{rowIndex})")
    public void clickEditCertificate(int rowIndex) {
        openActionsMenu(rowIndex);
        menuItemByNameExact("Edit certificate").click();
    }
}
