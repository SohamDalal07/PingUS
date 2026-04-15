package com.example.backtoyou;

import androidx.annotation.DrawableRes;

/**
 * Maps item category strings (Post / Firestore) to hero/list imagery.
 * Only campus photo drawables are used (no vector category icons).
 */
public final class CategoryDrawableHelper {

    private CategoryDrawableHelper() {}

    @DrawableRes
    public static int drawableResForCategory(String category, @DrawableRes int defaultRes) {
        String c = category == null ? "" : category.trim().toLowerCase();

        if (c.contains("electronics") || isElectronics(c)) return R.drawable.electronics;
        if (c.contains("cards & documents") || isDocuments(c)) return R.drawable.documents;
        if (c.contains("stationery & books") || isStationery(c)) return R.drawable.stationary;
        if (c.contains("keys & access") || isKeys(c)) return R.drawable.keys;
        if (c.contains("bags & clothing") || isBags(c)) return R.drawable.bags_clothing;
        if (c.contains("personal & others") || isPersonal(c)) return R.drawable.personal_others;
        return defaultRes == 0 ? R.drawable.electronics : defaultRes;
    }

    private static boolean isElectronics(String c) {
        return c.contains("phone") || c.contains("laptop") || c.contains("earphone")
                || c.contains("charger") || c.contains("smartwatch");
    }

    private static boolean isDocuments(String c) {
        return c.contains("id card") || c.contains("debit") || c.contains("library card")
                || c.contains("marksheet");
    }

    private static boolean isStationery(String c) {
        return c.contains("notebook") || c.contains("textbook") || c.contains("calculator")
                || c.contains("pen") || c.contains("scale");
    }

    private static boolean isKeys(String c) {
        return c.contains("key") || c.contains("padlock");
    }

    private static boolean isBags(String c) {
        return c.contains("bag") || c.contains("backpack") || c.contains("jacket")
                || c.contains("clothing") || c.contains("wallet");
    }

    private static boolean isPersonal(String c) {
        return c.contains("personal") || c.contains("other") || c.contains("umbrella")
                || c.contains("spectacle") || c.contains("medicine") || c.contains("lunchbox")
                || c.contains("bottle");
    }

    /** {@code true} for JPEG/PNG campus artwork (not vector icons). */
    public static boolean isCampusPhoto(@DrawableRes int res) {
        return res == R.drawable.electronics || res == R.drawable.documents
                || res == R.drawable.stationary || res == R.drawable.keys
                || res == R.drawable.bags_clothing || res == R.drawable.personal_others;
    }
}
