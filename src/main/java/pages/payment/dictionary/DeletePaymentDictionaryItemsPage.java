package pages.payment.dictionary;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class DeletePaymentDictionaryItemsPage {
    private final Page page;

    private final Locator pageTitle;
    private final Locator deleteButton;

    private final Locator table;
    private final Locator rows;

    private final Locator emptyState;

    public DeletePaymentDictionaryItemsPage(Page page) {
        this.page = page;

        this.pageTitle = page.locator("app-page-title h3.page-title, h3.page-title").first();

        // кнопка Delete на странице удаления (красная справа сверху)
        this.deleteButton = page.locator("app-dictionary-delete button:has-text('Delete')").first();

        // таблица выбранных элементов (на странице удаления)
        this.table = page.locator("app-dictionary-delete table[role='table'], app-dictionary-delete table").first();
        this.rows = table.locator("tbody tr.mat-mdc-row, tbody tr[role='row'], tbody tr");

        // empty state "No available dictionaries"
        this.emptyState = page.locator("app-dictionary-delete:has-text('No available dictionaries')").first();
    }

    public void waitOpened() {
        assertThat(pageTitle).hasText("Delete Payment Dictionary Items");
        assertThat(table).isVisible();
    }

    public int rowsCount() {
        return (int) rows.count();
    }

    public void clickDelete() {
        waitEnabled(deleteButton, 7_000);
        deleteButton.click();
    }

    public void waitEmptyState() {
        emptyState.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10_000));
        assertThat(emptyState).containsText("No available dictionaries");
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
}
