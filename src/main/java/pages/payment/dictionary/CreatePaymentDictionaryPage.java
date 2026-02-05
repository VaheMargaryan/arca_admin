package pages.payment.dictionary;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class CreatePaymentDictionaryPage {
    private final Page page;

    // ===== Локаторы страницы (общие) =====

    // Заголовок страницы: "Create Payment Dictionary Items"
    private final Locator pageTitle;

    // Кнопка Save
    private final Locator saveButton;

    // Кнопка "+ Add Dictionary" (добавляет новую строку формы)
    private final Locator addDictionaryButton;

    // ===== Конструктор =====

    public CreatePaymentDictionaryPage(Page page) {
        this.page = page;

        this.pageTitle = page.locator("app-page-title h3.page-title, h3.page-title").first();

        // Надёжнее через role (кнопка может быть disabled — всё равно найдётся)
        this.saveButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Save").setExact(true)
        );

        this.addDictionaryButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Add Dictionary")
        );
    }

    // ===== Ожидания / проверки =====

    /**
     * Ждём, что открылась нужная страница и хотя бы одна строка формы есть на экране.
     */
    public void waitOpened() {
        assertThat(pageTitle).hasText("Create Payment Dictionary Items");

        // Первая строка: Dictionary Key input должен стать видимым
        dictionaryKeyInputs()
                .first()
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    // ===== Локаторы строк (по индексам) =====

    // Dictionary Key* (input) — один на строку
    private Locator dictionaryKeyInputs() {
        return page.locator("mat-form-field:has-text('Dictionary Key') input");
    }

    // Language* (mat-select) — один на строку
    private Locator languageSelects() {
        return page.locator("mat-form-field:has-text('Language') mat-select");
    }

    // Dictionary Entry ID* (input) — один на строку
    private Locator dictionaryEntryIdInputs() {
        return page.locator("mat-form-field:has-text('Dictionary Entry ID') input");
    }

    // Value* (input/textarea) — один на строку
    private Locator valueInputs() {
        return page.locator(
                "mat-form-field:has-text('Value') input, " +
                        "mat-form-field:has-text('Value') textarea"
        );
    }

    /**
     * Кнопка удаления строки (красный X справа).
     * На разных билдах может быть:
     * - mat-icon с текстом close
     * - svg/иконка + aria-label
     * Поэтому делаем несколько fallback-ов и ограничиваем область form'ой.
     */
    private Locator removeRowButtons() {
        return page.locator(
                "app-dictionary-create-edit form button:has(mat-icon:has-text('close')), " +
                        "app-dictionary-create-edit form button:has([data-mat-icon-name='close']), " +
                        "app-dictionary-create-edit form button:has-text('×'), " +
                        "app-dictionary-create-edit form button:has-text('x')"
        );
    }

    // Панель выпадающего списка mat-select (когда Language открыт)
    private Locator visibleSelectPanel() {
        return page.locator("div.mat-mdc-select-panel:visible, div.mat-mdc-select-panel:visible").first();
    }

    // ===== Публичные методы (действия) =====

    public int rowsCount() {
        return (int) dictionaryKeyInputs().count();
    }

    public void clickSave() {
        saveButton.click();
    }

    /**
     * Нажать "+ Add Dictionary" и дождаться, что строк стало больше на 1.
     */
    public void clickAddDictionaryRow() {
        int before = rowsCount();
        addDictionaryButton.click();
        waitRowsCount(before + 1, 5000);
    }

    /**
     * Удалить строку по индексу (красный X) и дождаться уменьшения количества строк.
     */
    public void removeRow(int rowIndex) {
        int before = rowsCount();
        removeRowButtons().nth(rowIndex).click();
        waitRowsCount(Math.max(0, before - 1), 5000);
    }

    // ===== Fill по строке =====

    public void fillDictionaryKey(int rowIndex, String value) {
        dictionaryKeyInputs().nth(rowIndex).fill(value);
    }

    public void fillDictionaryEntryId(int rowIndex, String value) {
        dictionaryEntryIdInputs().nth(rowIndex).fill(value);
    }

    public void fillValue(int rowIndex, String value) {
        valueInputs().nth(rowIndex).fill(value);
    }

    /**
     * Выбрать язык в выпадашке Language*.
     * На скрине опции: Armenian / English / Russian.
     */
    public void selectLanguage(int rowIndex, String language) {
        // Открываем mat-select
        languageSelects().nth(rowIndex).click();

        // Ждём панель
        Locator panel = visibleSelectPanel();
        panel.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        // В mat-select опции обычно имеют role=option
        panel.getByRole(
                AriaRole.OPTION,
                new Locator.GetByRoleOptions().setName(language).setExact(true)
        ).click();

        // Панель должна закрыться
        panel.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    /**
     * Удобный метод заполнить всю строку.
     */
    public void fillRow(int rowIndex, String dictKey, String language, String entryId, String value) {
        fillDictionaryKey(rowIndex, dictKey);
        selectLanguage(rowIndex, language);
        fillDictionaryEntryId(rowIndex, entryId);
        fillValue(rowIndex, value);
    }

    // ===== Внутренние ожидания =====

    private void waitRowsCount(int expected, int timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (rowsCount() == expected) return;
            page.waitForTimeout(100);
        }
        throw new IllegalStateException(
                "Rows count did not reach expected value. Expected=" + expected + ", actual=" + rowsCount()
        );
    }
}
