package pages.payment.dictionary;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class PaymentDictionaryListPage {
    private final Page page;

    private final Locator pageTitle;

    private final Locator createButton;
    private final Locator deleteSelectedButton;

    private final Locator table;
    private final Locator rows;

    public PaymentDictionaryListPage(Page page) {
        this.page = page;

        this.pageTitle = page.locator("app-page-title h3.page-title, h3.page-title").first();

        this.createButton = page.locator("button:has-text('Create'), button:has(span:has-text('Create'))").first();
        this.deleteSelectedButton = page.locator("button:has-text('Delete selected')").first();

        this.table = page.locator("main table[role='table'], table[role='table']").first();
        this.rows = table.locator("tbody tr.mat-mdc-row, tbody tr[role='row'], tbody tr");
    }

    public void waitOpened() {
        assertThat(pageTitle).hasText("Payment Dictionary List");
        assertThat(table).isVisible();
    }

    public void clickCreate() {
        createButton.click();
    }

    // ===== Поиск строк =====

    private Locator rowByEntryId(String entryId) {
        // entryId обычно лежит в колонке entryId -> mat-column-entryId
        Locator entryCell = page.locator("td.mat-column-entryId, td.cdk-column-entryId")
                .filter(new Locator.FilterOptions().setHasText(entryId));

        return rows.filter(new Locator.FilterOptions().setHas(entryCell)).first();
    }

    public boolean isEntryIdPresent(String entryId) {
        return rowByEntryId(entryId).count() > 0;
    }

    public void waitEntryIdVisible(String entryId) {
        rowByEntryId(entryId).waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10_000));
    }

    public void waitEntryIdDisappears(String entryId) {
        rowByEntryId(entryId).waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(10_000));
    }

    // ===== Выбор чекбоксов =====

    public void selectRowByEntryId(String entryId) {
        Locator row = rowByEntryId(entryId);
        row.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10_000));

        // материал чекбокс чаще кликают по mat-checkbox
        Locator checkbox = row.locator("mat-checkbox").first();
        if (checkbox.count() == 0) {
            checkbox = row.locator("[role='checkbox'], input[type='checkbox']").first();
        }

        checkbox.scrollIntoViewIfNeeded();
        checkbox.click(new Locator.ClickOptions().setForce(true));
    }

    // ===== Delete selected =====

    public void clickDeleteSelected() {
        waitEnabled(deleteSelectedButton, 7_000);
        deleteSelectedButton.click();
    }

    // ===== Удаление через иконку мусорки =====

    public void clickTrashDeleteByEntryId(String entryId) {
        Locator row = rowByEntryId(entryId);
        row.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10_000));

        Locator deleteBtn = row.locator("td.mat-column-actions, td.cdk-column-actions")
                .locator(
                        "button:has(mat-icon[aria-label='dictionary.table.delete']), " +
                                "button:has(mat-icon[fonticon='delete']), " +
                                "button:has(mat-icon:has-text('delete'))"
                )
                .first();

        deleteBtn.scrollIntoViewIfNeeded();
        deleteBtn.click();
    }

    public void confirmDeleteModal() {
        Locator confirm = page.locator(
                        "app-confirmation-dialog button:has-text('Confirm'), " +
                                "mat-dialog-container button:has-text('Confirm')"
                )
                .first();

        confirm.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(7_000));

        confirm.click();
    }

    // ===== util =====

    private void waitEnabled(Locator button, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try {
                if (button.isVisible() && button.isEnabled()) return;
            } catch (Exception ignored) {}
            page.waitForTimeout(100);
        }
        throw new AssertionError("Button was not enabled within " + timeoutMs + "ms");
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
}
