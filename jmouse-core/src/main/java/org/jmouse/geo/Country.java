package org.jmouse.geo;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 🌍 Country — ISO 3166-1 dataset (без залежності від {@code java.util.Locale}).
 *
 * <p>Формат константи: {@code XX("alpha2","alpha3","currency", callingCode, "English", "Local")}.</p>
 *
 * <p>Пошук:
 * <ul>
 *   <li>{@link #ofIso2(String)} — за ISO α2 (UA, US, DE)</li>
 *   <li>{@link #ofIso3(String)} — за ISO α3 (UKR, USA, DEU)</li>
 *   <li>{@link #ofCallingCode(int)} — за E.164 кодом (380, 1, 44)</li>
 *   <li>{@link #ofCurrency(String)} — за ISO-4217 (UAH, USD, EUR)</li>
 *   <li>{@link #ofName(String)} / {@link #ofLocalName(String)} — за назвою</li>
 *   <li>{@link #of(String)} — "розумний" пошук: спробує α2 → α3 → англ.назву (точний збіг)</li>
 * </ul>
 * </p>
 *
 * <p>⚠️ Заувага: {@link #ofCurrency(String)} може повертати кілька країн (напр., EUR).</p>
 */
public enum Country {

    // Europe
    AL("AL","ALB","ALL",355,"Albania","Shqipëria"),
    AD("AD","AND","EUR",376,"Andorra","Andorra"),
    AT("AT","AUT","EUR",43,"Austria","Österreich"),
    BY("BY","BLR","BYN",375,"Belarus","Беларусь"),
    BE("BE","BEL","EUR",32,"Belgium","België"),
    BA("BA","BIH","BAM",387,"Bosnia and Herzegovina","Bosna i Hercegovina"),
    BG("BG","BGR","BGN",359,"Bulgaria","България"),
    HR("HR","HRV","EUR",385,"Croatia","Hrvatska"),
    CY("CY","CYP","EUR",357,"Cyprus","Κύπρος"),
    CZ("CZ","CZE","CZK",420,"Czechia","Česko"),
    DK("DK","DNK","DKK",45,"Denmark","Danmark"),
    EE("EE","EST","EUR",372,"Estonia","Eesti"),
    FI("FI","FIN","EUR",358,"Finland","Suomi"),
    FR("FR","FRA","EUR",33,"France","France"),
    DE("DE","DEU","EUR",49,"Germany","Deutschland"),
    GR("GR","GRC","EUR",30,"Greece","Ελλάδα"),
    HU("HU","HUN","HUF",36,"Hungary","Magyarország"),
    IS("IS","ISL","ISK",354,"Iceland","Ísland"),
    IE("IE","IRL","EUR",353,"Ireland","Éire"),
    IT("IT","ITA","EUR",39,"Italy","Italia"),
    LV("LV","LVA","EUR",371,"Latvia","Latvija"),
    LI("LI","LIE","CHF",423,"Liechtenstein","Liechtenstein"),
    LT("LT","LTU","EUR",370,"Lithuania","Lietuva"),
    LU("LU","LUX","EUR",352,"Luxembourg","Luxembourg"),
    MT("MT","MLT","EUR",356,"Malta","Malta"),
    MD("MD","MDA","MDL",373,"Moldova","Republica Moldova"),
    MC("MC","MCO","EUR",377,"Monaco","Monaco"),
    ME("ME","MNE","EUR",382,"Montenegro","Crna Gora"),
    NL("NL","NLD","EUR",31,"Netherlands","Nederland"),
    MK("MK","MKD","MKD",389,"North Macedonia","Северна Македонија"),
    NO("NO","NOR","NOK",47,"Norway","Norge"),
    PL("PL","POL","PLN",48,"Poland","Polska"),
    PT("PT","PRT","EUR",351,"Portugal","Portugal"),
    RO("RO","ROU","RON",40,"Romania","România"),
    RU("RU","RUS","RUB",7,"Russia","Россия"),
    SM("SM","SMR","EUR",378,"San Marino","San Marino"),
    RS("RS","SRB","RSD",381,"Serbia","Србија"),
    SK("SK","SVK","EUR",421,"Slovakia","Slovensko"),
    SI("SI","SVN","EUR",386,"Slovenia","Slovenija"),
    ES("ES","ESP","EUR",34,"Spain","España"),
    SE("SE","SWE","SEK",46,"Sweden","Sverige"),
    CH("CH","CHE","CHF",41,"Switzerland","Schweiz"),
    TR("TR","TUR","TRY",90,"Turkey","Türkiye"),
    UA("UA","UKR","UAH",380,"Ukraine","Україна"),
    GB("GB","GBR","GBP",44,"United Kingdom","United Kingdom"),
    VA("VA","VAT","EUR",379,"Vatican City","Città del Vaticano"),
    // Asia
    AF("AF","AFG","AFN",93,"Afghanistan","افغانستان"),
    AM("AM","ARM","AMD",374,"Armenia","Հայաստան"),
    AZ("AZ","AZE","AZN",994,"Azerbaijan","Azərbaycan"),
    BH("BH","BHR","BHD",973,"Bahrain","البحرين"),
    BD("BD","BGD","BDT",880,"Bangladesh","বাংলাদেশ"),
    BT("BT","BTN","INR",975,"Bhutan","འབྲུག"),
    BN("BN","BRN","BND",673,"Brunei","Brunei"),
    KH("KH","KHM","KHR",855,"Cambodia","កម្ពុជា"),
    CN("CN","CHN","CNY",86,"China","中国"),
    GE("GE","GEO","GEL",995,"Georgia","საქართველო"),
    IN("IN","IND","INR",91,"India","भारत"),
    ID("ID","IDN","IDR",62,"Indonesia","Indonesia"),
    IR("IR","IRN","IRR",98,"Iran","ایران"),
    IQ("IQ","IRQ","IQD",964,"Iraq","العراق"),
    IL("IL","ISR","ILS",972,"Israel","ישראל"),
    JP("JP","JPN","JPY",81,"Japan","日本"),
    JO("JO","JOR","JOD",962,"Jordan","الأردن"),
    KZ("KZ","KAZ","KZT",7,"Kazakhstan","Қазақстан"),
    KW("KW","KWT","KWD",965,"Kuwait","الكويت"),
    KG("KG","KGZ","KGS",996,"Kyrgyzstan","Кыргызстан"),
    LA("LA","LAO","LAK",856,"Laos","ລາວ"),
    LB("LB","LBN","LBP",961,"Lebanon","لبنان"),
    MY("MY","MYS","MYR",60,"Malaysia","Malaysia"),
    MV("MV","MDV","MVR",960,"Maldives","ދިވެހި"),
    MN("MN","MNG","MNT",976,"Mongolia","Монгол улс"),
    MM("MM","MMR","MMK",95,"Myanmar","မြန်မာ"),
    NP("NP","NPL","NPR",977,"Nepal","नेपाल"),
    KP("KP","PRK","KPW",850,"North Korea","조선"),
    OM("OM","OMN","OMR",968,"Oman","عمان"),
    PK("PK","PAK","PKR",92,"Pakistan","پاکستان"),
    PS("PS","PSE","ILS",970,"Palestine","فلسطين"),
    PH("PH","PHL","PHP",63,"Philippines","Pilipinas"),
    QA("QA","QAT","QAR",974,"Qatar","قطر"),
    SA("SA","SAU","SAR",966,"Saudi Arabia","السعودية"),
    SG("SG","SGP","SGD",65,"Singapore","Singapore"),
    KR("KR","KOR","KRW",82,"South Korea","대한민국"),
    LK("LK","LKA","LKR",94,"Sri Lanka","ශ්‍රී ලංකාව"),
    SY("SY","SYR","SYP",963,"Syria","سوريا"),
    TJ("TJ","TJK","TJS",992,"Tajikistan","Тоҷикистон"),
    TH("TH","THA","THB",66,"Thailand","ไทย"),
    TL("TL","TLS","USD",670,"Timor-Leste","Timor-Leste"),
    TM("TM","TKM","TMT",993,"Turkmenistan","Türkmenistan"),
    AE("AE","ARE","AED",971,"United Arab Emirates","الإمارات"),
    UZ("UZ","UZB","UZS",998,"Uzbekistan","Oʻzbekiston"),
    VN("VN","VNM","VND",84,"Vietnam","Việt Nam"),
    YE("YE","YEM","YER",967,"Yemen","اليمن"),
    // Africa
    DZ("DZ","DZA","DZD",213,"Algeria","الجزائر"),
    AO("AO","AGO","AOA",244,"Angola","Angola"),
    BJ("BJ","BEN","XOF",229,"Benin","Bénin"),
    BW("BW","BWA","BWP",267,"Botswana","Botswana"),
    BF("BF","BFA","XOF",226,"Burkina Faso","Burkina Faso"),
    BI("BI","BDI","BIF",257,"Burundi","Uburundi"),
    CV("CV","CPV","CVE",238,"Cabo Verde","Cabo Verde"),
    CM("CM","CMR","XAF",237,"Cameroon","Cameroun"),
    CF("CF","CAF","XAF",236,"Central African Republic","République centrafricaine"),
    TD("TD","TCD","XAF",235,"Chad","Tchad"),
    KM("KM","COM","KMF",269,"Comoros","جزر القمر"),
    CD("CD","COD","CDF",243,"Congo (Democratic Republic)","République démocratique du Congo"),
    CG("CG","COG","XAF",242,"Congo (Republic)","République du Congo"),
    CI("CI","CIV","XOF",225,"Côte d'Ivoire","Côte d’Ivoire"),
    DJ("DJ","DJI","DJF",253,"Djibouti","جيبوتي"),
    EG("EG","EGY","EGP",20,"Egypt","مصر"),
    GQ("GQ","GNQ","XAF",240,"Equatorial Guinea","Guinea Ecuatorial"),
    ER("ER","ERI","ERN",291,"Eritrea","ኤርትራ"),
    SZ("SZ","SWZ","SZL",268,"Eswatini","eSwatini"),
    ET("ET","ETH","ETB",251,"Ethiopia","ኢትዮጵያ"),
    GA("GA","GAB","XAF",241,"Gabon","Gabon"),
    GM("GM","GMB","GMD",220,"Gambia","Gambia"),
    GH("GH","GHA","GHS",233,"Ghana","Ghana"),
    GN("GN","GIN","GNF",224,"Guinea","Guinée"),
    GW("GW","GNB","XOF",245,"Guinea-Bissau","Guiné-Bissau"),
    KE("KE","KEN","KES",254,"Kenya","Kenya"),
    LS("LS","LSO","LSL",266,"Lesotho","Lesotho"),
    LR("LR","LBR","LRD",231,"Liberia","Liberia"),
    LY("LY","LBY","LYD",218,"Libya","ليبيا"),
    MG("MG","MDG","MGA",261,"Madagascar","Madagasikara"),
    MW("MW","MWI","MWK",265,"Malawi","Malawi"),
    ML("ML","MLI","XOF",223,"Mali","Mali"),
    MR("MR","MRT","MRU",222,"Mauritania","موريتانيا"),
    MU("MU","MUS","MUR",230,"Mauritius","Mauritius"),
    MA("MA","MAR","MAD",212,"Morocco","المغرب"),
    MZ("MZ","MOZ","MZN",258,"Mozambique","Moçambique"),
    NA("NA","NAM","NAD",264,"Namibia","Namibia"),
    NE("NE","NER","XOF",227,"Niger","Niger"),
    NG("NG","NGA","NGN",234,"Nigeria","Nigeria"),
    RW("RW","RWA","RWF",250,"Rwanda","Rwanda"),
    ST("ST","STP","STN",239,"São Tomé and Príncipe","São Tomé e Príncipe"),
    SN("SN","SEN","XOF",221,"Senegal","Sénégal"),
    SC("SC","SYC","SCR",248,"Seychelles","Seychelles"),
    SL("SL","SLE","SLL",232,"Sierra Leone","Sierra Leone"),
    SO("SO","SOM","SOS",252,"Somalia","Soomaaliya"),
    ZA("ZA","ZAF","ZAR",27,"South Africa","South Africa"),
    SS("SS","SSD","SSP",211,"South Sudan","South Sudan"),
    SD("SD","SDN","SDG",249,"Sudan","السودان"),
    TZ("TZ","TZA","TZS",255,"Tanzania","Tanzania"),
    TG("TG","TGO","XOF",228,"Togo","Togo"),
    TN("TN","TUN","TND",216,"Tunisia","تونس"),
    UG("UG","UGA","UGX",256,"Uganda","Uganda"),
    ZM("ZM","ZMB","ZMW",260,"Zambia","Zambia"),
    ZW("ZW","ZWE","ZWL",263,"Zimbabwe","Zimbabwe"),
    // America
    AG("AG","ATG","XCD",1,"Antigua and Barbuda","Antigua and Barbuda"),
    AR("AR","ARG","ARS",54,"Argentina","Argentina"),
    BS("BS","BHS","BSD",1,"Bahamas","Bahamas"),
    BB("BB","BRB","BBD",1,"Barbados","Barbados"),
    BZ("BZ","BLZ","BZD",501,"Belize","Belize"),
    BO("BO","BOL","BOB",591,"Bolivia","Bolivia"),
    BR("BR","BRA","BRL",55,"Brazil","Brasil"),
    CA("CA","CAN","CAD",1,"Canada","Canada"),
    CL("CL","CHL","CLP",56,"Chile","Chile"),
    CO("CO","COL","COP",57,"Colombia","Colombia"),
    CR("CR","CRI","CRC",506,"Costa Rica","Costa Rica"),
    CU("CU","CUB","CUP",53,"Cuba","Cuba"),
    DM("DM","DMA","XCD",1,"Dominica","Dominica"),
    DO("DO","DOM","DOP",1,"Dominican Republic","República Dominicana"),
    EC("EC","ECU","USD",593,"Ecuador","Ecuador"),
    SV("SV","SLV","USD",503,"El Salvador","El Salvador"),
    GD("GD","GRD","XCD",1,"Grenada","Grenada"),
    GT("GT","GTM","GTQ",502,"Guatemala","Guatemala"),
    GY("GY","GUY","GYD",592,"Guyana","Guyana"),
    HT("HT","HTI","HTG",509,"Haiti","Haïti"),
    HN("HN","HND","HNL",504,"Honduras","Honduras"),
    JM("JM","JAM","JMD",1,"Jamaica","Jamaica"),
    MX("MX","MEX","MXN",52,"Mexico","México"),
    NI("NI","NIC","NIO",505,"Nicaragua","Nicaragua"),
    PA("PA","PAN","PAB",507,"Panama","Panamá"),
    PY("PY","PRY","PYG",595,"Paraguay","Paraguay"),
    PE("PE","PER","PEN",51,"Peru","Perú"),
    KN("KN","KNA","XCD",1,"Saint Kitts and Nevis","Saint Kitts and Nevis"),
    LC("LC","LCA","XCD",1,"Saint Lucia","Saint Lucia"),
    VC("VC","VCT","XCD",1,"Saint Vincent and the Grenadines","Saint Vincent and the Grenadines"),
    SR("SR","SUR","SRD",597,"Suriname","Suriname"),
    TT("TT","TTO","TTD",1,"Trinidad and Tobago","Trinidad and Tobago"),
    US("US","USA","USD",1,"United States","United States"),
    UY("UY","URY","UYU",598,"Uruguay","Uruguay"),
    VE("VE","VEN","VES",58,"Venezuela","Venezuela"),
    //  Oceania
    AU("AU","AUS","AUD",61,"Australia","Australia"),
    FJ("FJ","FJI","FJD",679,"Fiji","Fiji"),
    KI("KI","KIR","AUD",686,"Kiribati","Kiribati"),
    MH("MH","MHL","USD",692,"Marshall Islands","Marshall Islands"),
    FM("FM","FSM","USD",691,"Micronesia","Micronesia"),
    NR("NR","NRU","AUD",674,"Nauru","Nauru"),
    NZ("NZ","NZL","NZD",64,"New Zealand","New Zealand"),
    PW("PW","PLW","USD",680,"Palau","Palau"),
    PG("PG","PNG","PGK",675,"Papua New Guinea","Papua New Guinea"),
    WS("WS","WSM","WST",685,"Samoa","Samoa"),
    SB("SB","SLB","SBD",677,"Solomon Islands","Solomon Islands"),
    TO("TO","TON","TOP",676,"Tonga","Tonga"),
    TV("TV","TUV","AUD",688,"Tuvalu","Tuvalu"),
    VU("VU","VUT","VUV",678,"Vanuatu","Vanuatu");

    private final String iso2Code;
    private final String iso3Code;
    private final String currency;    // ISO-4217
    private final int    callingCode; // E.164
    private final String englishName;
    private final String localName;

    Country(String iso2Code, String iso3Code, String currency, int callingCode, String name, String localName) {
        this.iso2Code = Objects.requireNonNull(iso2Code, "alpha2");
        this.iso3Code = Objects.requireNonNull(iso3Code, "iso3Code");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.callingCode = callingCode;
        this.englishName = Objects.requireNonNull(name, "name");
        this.localName = Objects.requireNonNull(localName, "localName");
    }

    /** ISO 3166-1 alpha-2 (e.g., UA). */
    public String getIso2Code() { return iso2Code; }

    /** ISO 3166-1 alpha-3 (e.g., UKR). */
    public String getIso3Code() { return iso3Code; }

    /** ISO-4217 currency code (e.g., UAH). */
    public String getCurrency() { return currency; }

    /** E.164 calling code (e.g., 380). */
    public int getCallingCode() { return callingCode; }

    /** English country name (e.g., Ukraine). */
    public String getName() { return englishName; }

    /** Local name (e.g., Україна). */
    public String getLocalName() { return localName; }

    // --------------------------
    // Indexes for fast lookups
    // --------------------------

    private static final Map<String, Country> ISO2_INDEX = Collections.unmodifiableMap(
            indexBy(Country::getIso2Code, false));

    private static final Map<String, Country> ISO3_INDEX = Collections.unmodifiableMap(
            indexBy(Country::getIso3Code, false));

    private static final Map<Integer, List<Country>> CALLING_CODE_INDEX = Collections.unmodifiableMap(
            Arrays.stream(values()).collect(Collectors.groupingBy(Country::getCallingCode)));

    private static final Map<String, List<Country>> CURRENCY_INDEX = Collections.unmodifiableMap(
            Arrays.stream(values()).collect(Collectors.groupingBy(c -> normalize(c.getCurrency()))));

    private static final Map<String, Country> ENGLISH_NAME_INDEX = Collections.unmodifiableMap(
            indexBy(Country::getName, true));

    private static final Map<String, Country> LOCAL_NAME_INDEX = Collections.unmodifiableMap(
            indexBy(Country::getLocalName, true));

    private static Map<String, Country> indexBy(Function<Country, String> getter, boolean caseInsensitive) {
        return Arrays.stream(values()).collect(
                Collectors.toMap(c -> caseInsensitive ? normalize(getter.apply(c)) : getter.apply(c),
                                 Function.identity(), (a, b) -> a, LinkedHashMap::new
                )
        );
    }

    private static String normalize(String string) {
        return string == null ? "" : string.trim().toUpperCase(Locale.ROOT);
    }

    // --------------------------
    // Lookup API
    // --------------------------

    /**
     * 🔎 Smart lookup: tries alpha-2 → alpha-3 → exact English name (case-insensitive).
     * <pre>
     * Country.of("UA")    -> UA
     * Country.of("UKR")   -> UA
     * Country.of("Ukraine")-> UA
     * Country.of("Україна")-> UA
     * </pre>
     */
    public static Optional<Country> of(String any) {
        String            key      = normalize(any);
        Optional<Country> optional = ofIso2(key);

        if (optional.isPresent()) {
            return optional;
        }

        optional = ofIso3(key);
        if (optional.isPresent()) {
            return optional;
        }

        optional = ofName(key);
        if (optional.isPresent()) {
            return optional;
        }

        return ofLocalName(key);
    }

    /** 🔤 Find by ISO alpha-2 (case-insensitive). */
    public static Optional<Country> ofIso2(String iso2) {
        return Optional.ofNullable(ISO2_INDEX.get(normalize(iso2)));
    }

    /** 🔤 Find by ISO alpha-3 (case-insensitive). */
    public static Optional<Country> ofIso3(String iso3) {
        return Optional.ofNullable(ISO3_INDEX.get(normalize(iso3)));
    }

    /** ☎️ Countries with given E.164 calling code. */
    public static List<Country> ofCallingCode(int code) {
        return CALLING_CODE_INDEX.getOrDefault(code, List.of());
    }

    /** 💱 Countries using the given ISO-4217 currency (case-insensitive). */
    public static List<Country> ofCurrency(String currency) {
        return CURRENCY_INDEX.getOrDefault(normalize(currency), List.of());
    }

    /** 🇬🇧 Exact English name match (case-insensitive). */
    public static Optional<Country> ofName(String englishName) {
        return Optional.ofNullable(ENGLISH_NAME_INDEX.get(normalize(englishName)));
    }

    /** 🏷️ Exact local name match (case-insensitive). */
    public static Optional<Country> ofLocalName(String localName) {
        if (localName == null) return Optional.empty();
        return Optional.ofNullable(LOCAL_NAME_INDEX.get(normalize(localName)));
    }

    /** Returns immutable list of all countries (stable order). */
    public static List<Country> list() {
        return List.of(values());
    }

    @Override
    public String toString() {
        return iso2Code + "(" + iso2Code + "," + iso3Code + "," + currency + "," + callingCode + "," + englishName + "," + localName + ")";
    }
}
