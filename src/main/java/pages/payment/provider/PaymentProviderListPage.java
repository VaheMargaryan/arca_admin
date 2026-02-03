package pages.payment.provider;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.List;
import java.util.stream.Collectors;

public class PaymentProviderListPage {
    private final Page page;

    // ===== Локаторы (общие) =====
    // Таблица провайдеров (якорь для всех операций с таблицей)
    private final Locator table;

    // Кнопка Create (в правом верхнем углу страницы)
    private final Locator createButton;

    public PaymentProviderListPage(Page page) {
        this.page = page;

        // ===== Инициализация локаторов =====
        this.table = page.locator("app-provider-list table[role='table']");
        this.createButton = page.locator("app-provider-list button:has-text('Create')");
    }

    // ===== Приватные хелперы (таблица/строки/экшены) =====

    // Все строки таблицы (body)
    private Locator rows() {
        return table.locator("tbody tr[role='row']");
    }

    // Строка по индексу
    private Locator rowByIndex(int rowIndex) {
        return rows().nth(rowIndex);
    }

    // Ячейка Actions в конкретной строке
    private Locator actionsCell(Locator row) {
        return row.locator("td.mat-column-actions");
    }

    // Кнопка "три точки" (открывает edit-меню) в конкретной строке
    private Locator kebabMenuButton(Locator row) {
        return actionsCell(row).locator("button.mat-mdc-menu-trigger");
    }

    // Видимая панель открытого меню
    private Locator visibleMenuPanel() {
        return page.locator("div.mat-mdc-menu-panel:visible");
    }

    // Пункт меню по тексту (Edit, Edit accounts, ...)
    private Locator menuItemByText(String text) {
        return visibleMenuPanel()
                .getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName(text).setExact(true));
    }

    // Открыть edit-меню для строки и дождаться появления меню
    private void openEditMenuByRowIndex(int rowIndex) {
        Locator row = rowByIndex(rowIndex);
        kebabMenuButton(row).click();

        // Ожидаем, что меню реально открылось
        visibleMenuPanel().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    // ===== Методы (кнопки/меню) =====

    // Click Create
    public void clickCreateButton() {
        createButton.click();
    }

    // ===== Методы edit-меню (для любой строки таблицы) =====

    // Click "Edit"
    public void clickEdit(int rowIndex) {
        openEditMenuByRowIndex(rowIndex);
        menuItemByText("Edit").click();
    }

    // Click "Edit external connections"
    public void clickEditExternalConnections(int rowIndex) {
        openEditMenuByRowIndex(rowIndex);
        menuItemByText("Edit external connections").click();
    }

    // Click "Edit accounts"
    public void clickEditAccounts(int rowIndex) {
        openEditMenuByRowIndex(rowIndex);
        menuItemByText("Edit accounts").click();
    }

    // Click "Edit service identifiers"
    public void clickEditServiceIdentifiers(int rowIndex) {
        openEditMenuByRowIndex(rowIndex);
        menuItemByText("Edit service identifiers").click();
    }

    // Click "Edit certificate"
    public void clickEditCertificate(int rowIndex) {
        openEditMenuByRowIndex(rowIndex);
        menuItemByText("Edit certificate").click();
    }

    // Click "Edit currencies"
    public void clickEditCurrencies(int rowIndex) {
        openEditMenuByRowIndex(rowIndex);
        menuItemByText("Edit currencies").click();
    }

    // ===== Методы работы с таблицей =====

    /**
     * Возвращает локатор всех ячеек (td) указанной колонки.
     * columnKey — это часть после "mat-column-" в DevTools.
     * Примеры: "providerName", "countryId"
     */
    public Locator columnCells(String columnKey) {
        return table.locator("tbody tr td.mat-column-" + columnKey);
    }

    /**
     * Возвращает список текстов из указанной колонки.
     * Перед чтением ждёт, пока появится первая ячейка колонки (значит таблица уже отрисована/заполнена).
     */
    public List<String> columnTexts(String columnKey) {
        // Ожидание появления данных: первая ячейка колонки должна стать видимой
        columnCells(columnKey)
                .first()
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        // Получение текстов всех ячеек колонки и очистка пробелов по краям
        return columnCells(columnKey)
                .allTextContents()
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
