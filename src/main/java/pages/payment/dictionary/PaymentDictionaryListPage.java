package pages.payment.dictionary;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class PaymentDictionaryListPage {
    private final Page page;

    // ===== Локаторы страницы (общие) =====

    // Заголовок страницы (на скрине: "Payment Dictionary List")
    private final Locator pageTitle;

    // Таблица (якорь для всех операций со строками/ячейками)
    private final Locator table;

    // Кнопка Create (в правом верхнем углу)
    private final Locator createButton;

    // Bulk-кнопки (активируются после выбора строк)
    private final Locator deleteSelectedButton;
    private final Locator editSelectedButton;

    public PaymentDictionaryListPage(Page page) {
        this.page = page;

        // Заголовок (как в твоих Provider/Merchant)
        this.pageTitle = page.locator("app-page-title h3.page-title, h3.page-title").first();

        // Таблица: подстраховка на разные контейнеры
        this.table = page.locator("main table[role='table'], table[role='table']").first();

        // Create — чаще всего реально имеет текст Create
        this.createButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Create").setExact(true)
        );

        // Delete selected / Edit Selected — по тексту кнопок на скрине
        this.deleteSelectedButton = page.locator("button:has-text('Delete selected')").first();
        this.editSelectedButton = page.locator("button:has-text('Edit Selected')").first();
    }

    // ===== Ожидания / проверки =====

    /**
     * Ждём, что открылась нужная страница и таблица видима.
     */
    public void waitOpened() {
        assertThat(pageTitle).hasText("Payment Dictionary List");
        table.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    // ===== Хелперы: строки / чекбоксы / actions =====

    /**
     * Все строки таблицы (tbody).
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
     * Чекбокс в строке.
     * В Material обычно внутри td есть input[type=checkbox].
     */
    private Locator rowCheckbox(Locator row) {
        // На скрине первый столбец — чекбоксы
        return row.locator("input[type='checkbox']").first();
    }

    /**
     * Header checkbox (выделить все).
     */
    private Locator headerCheckbox() {
        return table.locator("thead input[type='checkbox']").first();
    }

    /**
     * Ячейка Actions в строке.
     */
    private Locator actionsCell(Locator row) {
        return row.locator("td.mat-column-actions, td.cdk-column-actions, td:last-child").first();
    }

    /**
     * Кнопка delete (иконка корзины) в Actions.
     * Делаем несколько вариантов, т.к. в DOM может быть:
     * - mat-icon[data-mat-icon-name='delete']
     * - mat-icon с текстом 'delete'
     * - aria-label со словом delete
     */
    private Locator deleteRowButton(Locator row) {
        Locator cell = actionsCell(row);

        // 1) самый желательный вариант: data-mat-icon-name="delete"
        Locator byDataName = cell.locator("button:has(mat-icon[data-mat-icon-name='delete'])").first();
        // 2) у некоторых mat-icon “delete” лежит текстом
        Locator byTextIcon = cell.locator("button:has(mat-icon:has-text('delete'))").first();
        // 3) aria-label содержит delete (на всякий)
        Locator byAria = cell.locator("button[aria-label*='delete' i], button:has(mat-icon[aria-label*='delete' i])").first();

        // Возвращаем первый “живой” (в момент вызова он ещё может быть не видим — ок, клик подождёт)
        return byDataName.or(byTextIcon).or(byAria).first();
    }

    // ===== Методы: верхние кнопки =====

    public void clickCreate() {
        createButton.click();
    }

    public void clickDeleteSelected() {
        deleteSelectedButton.click();
    }

    public void clickEditSelected() {
        editSelectedButton.click();
    }

    // ===== Методы: выбор строк =====

    public void selectRow(int rowIndex) {
        Locator cb = rowCheckbox(rowByIndex(rowIndex));
        if (!cb.isChecked()) cb.check();
    }

    public void unselectRow(int rowIndex) {
        Locator cb = rowCheckbox(rowByIndex(rowIndex));
        if (cb.isChecked()) cb.uncheck();
    }

    public void selectAll() {
        Locator cb = headerCheckbox();
        if (!cb.isChecked()) cb.check();
    }

    public void unselectAll() {
        Locator cb = headerCheckbox();
        if (cb.isChecked()) cb.uncheck();
    }

    // ===== Методы: действия в строке =====

    /**
     * Клик по корзине (delete) в строке.
     * Обычно после этого появляется confirm dialog — его уже проверяешь в тесте.
     */
    public void clickDeleteRow(int rowIndex) {
        Locator row = rowByIndex(rowIndex);
        deleteRowButton(row).click();
    }

    // ===== Методы: работа с колонками (универсально через mat-column-<key>) =====

    /**
     * Возвращает локатор всех ячеек (td) указанной колонки.
     * columnKey — часть после "mat-column-" в DevTools.
     * Примеры (предположительно по скрину):
     * "keyId", "entryId", "languageId", "value", "behavior", "id", "actions"
     */
    public Locator columnCells(String columnKey) {
        return table.locator("tbody tr td.mat-column-" + columnKey);
    }

    /**
     * Тексты из колонки. Ждём, что первая ячейка стала видимой => данные подгрузились.
     */
    public List<String> columnTexts(String columnKey) {
        Locator firstCell = columnCells(columnKey).first();
        firstCell.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        return columnCells(columnKey)
                .allTextContents()
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * Удобный хелпер: текст конкретной ячейки в строке.
     */
    public String cellText(int rowIndex, String columnKey) {
        Locator cell = rowByIndex(rowIndex).locator("td.mat-column-" + columnKey).first();
        cell.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return cell.innerText().trim();
    }

    public int getDictionaryIdByEntryId(String entryId) {
        // Таблица уже должна быть видима (waitOpened)
        Locator row = table.locator("tbody tr")
                .filter(new Locator.FilterOptions().setHasText(entryId))
                .first();

        row.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(15_000));

        String idText = row.locator("td.mat-column-id, td.cdk-column-id").first().innerText().trim();
        return Integer.parseInt(idText);
    }

    // Найти ID созданного Dictionary Item по EntryId (и Value для надёжности)
    public int findCreatedIdByEntryIdAndValue(String entryId, String value) {
        // Подстрой селектор table под свой (ниже максимально универсально)
        Locator table = page.locator("main table[role='table'], table[role='table']").first();
        Locator rows = table.locator("tbody tr");

        Locator row = rows
                .filter(new Locator.FilterOptions().setHasText(entryId))
                .filter(new Locator.FilterOptions().setHasText(value))
                .first();

        row.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10_000));

        // Колонка ID чаще всего mat-column-id
        Locator idCell = row.locator(
                "td.mat-column-id, td.mat-column-ID, td.cdk-column-id, td.cdk-column-ID"
        ).first();

        idCell.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5_000));

        String idText = idCell.innerText().trim();
        try {
            return Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Не смог распарсить ID из колонки ID. Text='" + idText + "'");
        }
    }


}
