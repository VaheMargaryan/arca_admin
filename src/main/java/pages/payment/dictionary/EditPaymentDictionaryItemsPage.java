package pages.payment.dictionary;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class EditPaymentDictionaryItemsPage {

    private final Page page;

    // максимально “живучие” локаторы под твой UI (по placeholder/role)
    private final Locator entryIdInputs;
    private final Locator valueInputs;
    private final Locator languageComboboxes;
    private final Locator saveButton;

    public EditPaymentDictionaryItemsPage(Page page) {
        this.page = page;

        this.entryIdInputs = page.locator(
                "mat-form-field:has-text('Dictionary Entry ID') input"
        );

        this.valueInputs = page.locator(
                "mat-form-field:has-text('Value') input, " +
                        "mat-form-field:has-text('Value') textarea"
        );

        // mat-select обычно даёт role=combobox
        this.languageComboboxes = page.locator(
                "mat-form-field:has-text('Language') [role='combobox'], " +
                        "mat-form-field:has-text('Language') mat-select"
        );

        this.saveButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save"));
    }

    public void waitOpened() {
        // заголовок может быть "Edit Payment Dictionary items"
        Locator header = page.locator("app-page-title h3.page-title, h3.page-title").first();
        header.waitFor(new Locator.WaitForOptions().setTimeout(10_000));
        assertThat(header).containsText("Edit");
    }

    public int rowsCount() {
        // считаем по количеству entryId инпутов (обычно 1 инпут = 1 строка)
        return entryIdInputs.count();
    }

    public void setRowLanguage(int rowIndex, String language) {
        Locator cb = languageComboboxes.nth(rowIndex);
        cb.scrollIntoViewIfNeeded();
        cb.click();

        Locator option = page.locator("mat-option, [role='option']")
                .filter(new Locator.FilterOptions().setHasText(language))
                .first();

        option.waitFor(new Locator.WaitForOptions().setTimeout(10_000));
        option.click();
    }

    public void setRowEntryId(int rowIndex, String entryId) {
        Locator input = entryIdInputs.nth(rowIndex);
        input.scrollIntoViewIfNeeded();
        input.fill(entryId);
        input.press("Tab"); // чтобы триггернуть blur/валидаторы
    }

    public void setRowValue(int rowIndex, String value) {
        Locator input = valueInputs.nth(rowIndex);
        input.scrollIntoViewIfNeeded();
        input.fill(value);
        input.press("Tab");
    }

    public String getRowEntryId(int rowIndex) {
        return entryIdInputs.nth(rowIndex).inputValue().trim();
    }

    public String getRowValue(int rowIndex) {
        return valueInputs.nth(rowIndex).inputValue().trim();
    }

    public String getRowLanguage(int rowIndex) {
        // часто text внутри combobox содержит выбранное значение
        return languageComboboxes.nth(rowIndex).innerText().trim();
    }

    public void clickSaveAndWaitList(Pattern listUrlPattern) {
        waitUntilEnabled(saveButton, 10_000);
        saveButton.click();
//        page.waitForURL(listUrlPattern, new Page.WaitForURLOptions().setTimeout(15_000));
    }

    private void waitUntilEnabled(Locator locator, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try {
                if (locator.isVisible() && locator.isEnabled()) return;
            } catch (Exception ignored) {}
            page.waitForTimeout(100);
        }
        throw new AssertionError("Element did not become enabled in " + timeoutMs + "ms");
    }
}
