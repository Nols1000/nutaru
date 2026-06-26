package com.github.nols1000.nutaru.packcompiler

/**
 * Synthetic regional products for the `demo` command.
 *
 * Stands up the 6 starter packs and the catalog structure without the
 * multi-GB OFF bulk export. Each region gets a handful of realistic,
 * region-typical SKUs so `demo` proves the full compile -> pack -> manifest
 * pipeline end to end. Production replaces these with `compile --input
 * <off-export.ndjson>`; see `docs/pack-compiler-spec.md` and issue #06.
 *
 * TODO: these are ~5 items per region, not the 50–100k the issue targets. The
 * compiler is the deliverable; the real item count needs the OFF export, which
 * is a release-time data-acquisition step documented in the spec.
 */
object DemoData {

    data class RegionPack(
        val meta: PackMeta,
        val products: List<Product>,
    )

    private const val BASE_URL = "https://github.com/nutaru/nutaru/releases/download/packs-v1"

    val regions: List<RegionPack> = listOf(
        region("us", "US starter pack", "US", us()),
        region("eu-mix", "EU-mix starter pack (DE/FR/ES/IT)", "EU-MIX", euMix()),
        region("uk", "UK starter pack", "UK", uk()),
        region("jp", "JP starter pack", "JP", jp()),
        region("br", "BR starter pack", "BR", br()),
        region("global", "Global brands starter pack", "GLOBAL", global()),
    )

    private fun region(id: String, name: String, region: String, products: List<Product>): RegionPack =
        RegionPack(PackMeta(id, name, "1.0.0", region, url = "$BASE_URL/$id.pack"), products)

    private fun p(
        id: Long,
        barcode: String,
        name: String,
        brand: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        categories: List<String> = emptyList(),
        serving: Serving? = null,
        ingredients: String? = null,
    ): Product = Product(
        id = id,
        barcode = barcode,
        nameI18n = sortedMapOf("en" to name),
        brand = brand,
        categories = categories.sorted(),
        kcalPer100g = kcal,
        proteinGPer100g = protein,
        carbsGPer100g = carbs,
        fatGPer100g = fat,
        servings = listOfNotNull(serving),
        ingredients = ingredients,
        sourceId = PackFormat.SOURCE_ID_OFF,
        license = PackFormat.LICENSE_ODBL,
        attribution = PackFormat.ATTRIBUTION_OFF,
    )

    private fun us() = listOf(
        p(1, "0049000002871", "Cheerios", "General Mills", 366.0, 7.2, 73.2, 5.4, listOf("breakfast-cereals"), Serving("1 cup (28g)", 28.0), "Whole grain oats, sugar, oat bran, corn starch..."),
        p(2, "0028400040016", "Jif Creamy Peanut Butter", "Jif", 588.0, 25.0, 17.0, 50.0, listOf("spreads", "peanut-butter"), Serving("2 tbsp (32g)", 32.0)),
        p(3, "0001200010019", "Coca-Cola Classic", "Coca-Cola", 42.0, 0.0, 10.6, 0.0, listOf("beverages", "carbonated-drinks"), Serving("1 can (355ml)", 355.0)),
        p(4, "0038000143717", "Oikos Greek Yogurt", "Dannon", 59.0, 10.0, 3.6, 0.4, listOf("dairy", "yogurts"), Serving("1 cup (150g)", 150.0)),
    )

    private fun euMix() = listOf(
        p(11, "3017620422003", "Nutella", "Ferrero", 539.0, 6.3, 57.5, 30.9, listOf("spreads", "chocolate"), Serving("15g", 15.0), "Sugar, palm oil, hazelnuts, cocoa, milk..."),
        p(12, "4008400152674", "Knorr Fix Spaghetti Bolognese", "Knorr", 312.0, 8.0, 52.0, 6.0, listOf("meal-kits", "sauces")),
        p(13, "8410000820011", "Galletas Maria", "Galletas Maria", 416.0, 6.7, 76.0, 11.0, listOf("biscuits"), Serving("1 cookie (5g)", 5.0)),
        p(14, "8076809513754", "Barilla Spaghetti No.5", "Barilla", 359.0, 12.0, 73.0, 1.5, listOf("pasta"), Serving("100g", 100.0)),
    )

    private fun uk() = listOf(
        p(21, "5010087000010", "Heinz Baked Beans", "Heinz", 79.0, 4.7, 9.8, 0.6, listOf("canned", "beans"), Serving("1/2 can (200g)", 200.0)),
        p(22, "5000159407236", "Walkers Ready Salted Crisps", "Walkers", 536.0, 6.0, 51.0, 33.0, listOf("snacks", "crisps"), Serving("25g", 25.0)),
        p(23, "0221050000000", "PG Tips Tea Bags", "PG Tips", 0.0, 0.0, 0.0, 0.0, listOf("beverages", "tea"), Serving("1 bag (2g)", 2.0)),
    )

    private fun jp() = listOf(
        p(31, "4901301172188", "Cup Noodle Soy Sauce", "Nissin", 188.0, 5.6, 26.0, 6.8, listOf("noodles", "instant"), Serving("1 cup (75g)", 75.0)),
        p(32, "4901872353031", "Pocky Chocolate", "Glico", 521.0, 8.3, 65.0, 25.0, listOf("snacks", "chocolate"), Serving("1 box (47g)", 47.0)),
        p(33, "4902430156118", "Calpis", "Calpis", 64.0, 1.0, 13.0, 0.6, listOf("beverages"), Serving("100ml", 100.0)),
    )

    private fun br() = listOf(
        p(41, "7891000053508", "Leite Integral", "Itambé", 61.0, 3.0, 4.7, 3.0, listOf("dairy", "milk"), Serving("200ml", 200.0)),
        p(42, "7891000372200", "Pão de Açúcar Arroz", "Pão de Açúcar", 366.0, 7.0, 80.0, 1.0, listOf("grains", "rice"), Serving("100g", 100.0)),
        p(43, "7891149410236", "Guaraná Antarctica", "Antarctica", 41.0, 0.0, 10.4, 0.0, listOf("beverages", "carbonated-drinks"), Serving("350ml", 350.0)),
    )

    private fun global() = listOf(
        p(51, "054000015283", "Nutella", "Ferrero", 539.0, 6.3, 57.5, 30.9, listOf("spreads", "chocolate"), Serving("15g", 15.0)),
        p(52, "5449000000996", "Coca-Cola Classic", "Coca-Cola", 42.0, 0.0, 10.6, 0.0, listOf("beverages", "carbonated-drinks"), Serving("1 can (330ml)", 330.0)),
        p(53, "7622210449283", "Oreo", "Nabisco", 481.0, 4.5, 71.0, 19.0, listOf("biscuits"), Serving("1 cookie (11g)", 11.0)),
    )
}
