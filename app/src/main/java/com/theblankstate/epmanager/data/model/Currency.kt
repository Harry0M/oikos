package com.theblankstate.epmanager.data.model

/**
 * Represents a currency with its code, name, symbol, and flag emoji.
 */
data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String
)

/**
 * Provides the complete list of world currencies.
 */
object CurrencyProvider {
    
    val currencies: List<Currency> = listOf(
        // Major Currencies
        Currency("USD", "US Dollar", "$", "🇺🇸"),
        Currency("EUR", "Euro", "€", "🇪🇺"),
        Currency("GBP", "British Pound", "£", "🇬🇧"),
        Currency("JPY", "Japanese Yen", "¥", "🇯🇵"),
        Currency("CNY", "Chinese Yuan", "¥", "🇨🇳"),
        Currency("INR", "Indian Rupee", "₹", "🇮🇳"),
        Currency("AUD", "Australian Dollar", "A$", "🇦🇺"),
        Currency("CAD", "Canadian Dollar", "C$", "🇨🇦"),
        Currency("CHF", "Swiss Franc", "Fr", "🇨🇭"),
        Currency("HKD", "Hong Kong Dollar", "HK$", "🇭🇰"),
        Currency("SGD", "Singapore Dollar", "S$", "🇸🇬"),
        Currency("NZD", "New Zealand Dollar", "NZ$", "🇳🇿"),
        Currency("KRW", "South Korean Won", "₩", "🇰🇷"),
        Currency("MXN", "Mexican Peso", "MX$", "🇲🇽"),
        Currency("BRL", "Brazilian Real", "R$", "🇧🇷"),
        Currency("RUB", "Russian Ruble", "₽", "🇷🇺"),
        Currency("ZAR", "South African Rand", "R", "🇿🇦"),
        Currency("TRY", "Turkish Lira", "₺", "🇹🇷"),
        Currency("SEK", "Swedish Krona", "kr", "🇸🇪"),
        Currency("NOK", "Norwegian Krone", "kr", "🇳🇴"),
        Currency("DKK", "Danish Krone", "kr", "🇩🇰"),
        Currency("PLN", "Polish Zloty", "zł", "🇵🇱"),
        Currency("THB", "Thai Baht", "฿", "🇹🇭"),
        Currency("IDR", "Indonesian Rupiah", "Rp", "🇮🇩"),
        Currency("MYR", "Malaysian Ringgit", "RM", "🇲🇾"),
        Currency("PHP", "Philippine Peso", "₱", "🇵🇭"),
        Currency("VND", "Vietnamese Dong", "₫", "🇻🇳"),
        Currency("AED", "UAE Dirham", "د.إ", "🇦🇪"),
        Currency("SAR", "Saudi Riyal", "﷼", "🇸🇦"),
        Currency("QAR", "Qatari Riyal", "﷼", "🇶🇦"),
        Currency("KWD", "Kuwaiti Dinar", "د.ك", "🇰🇼"),
        Currency("BHD", "Bahraini Dinar", "BD", "🇧🇭"),
        Currency("OMR", "Omani Rial", "﷼", "🇴🇲"),
        Currency("ILS", "Israeli Shekel", "₪", "🇮🇱"),
        Currency("EGP", "Egyptian Pound", "E£", "🇪🇬"),
        Currency("PKR", "Pakistani Rupee", "₨", "🇵🇰"),
        Currency("BDT", "Bangladeshi Taka", "৳", "🇧🇩"),
        Currency("LKR", "Sri Lankan Rupee", "Rs", "🇱🇰"),
        Currency("NPR", "Nepalese Rupee", "₨", "🇳🇵"),
        Currency("MMK", "Myanmar Kyat", "K", "🇲🇲"),
        Currency("KHR", "Cambodian Riel", "៛", "🇰🇭"),
        Currency("LAK", "Lao Kip", "₭", "🇱🇦"),
        Currency("TWD", "Taiwan Dollar", "NT$", "🇹🇼"),
        Currency("CZK", "Czech Koruna", "Kč", "🇨🇿"),
        Currency("HUF", "Hungarian Forint", "Ft", "🇭🇺"),
        Currency("RON", "Romanian Leu", "lei", "🇷🇴"),
        Currency("BGN", "Bulgarian Lev", "лв", "🇧🇬"),
        Currency("HRK", "Croatian Kuna", "kn", "🇭🇷"),
        Currency("RSD", "Serbian Dinar", "дин", "🇷🇸"),
        Currency("UAH", "Ukrainian Hryvnia", "₴", "🇺🇦"),
        Currency("BYN", "Belarusian Ruble", "Br", "🇧🇾"),
        Currency("KZT", "Kazakhstani Tenge", "₸", "🇰🇿"),
        Currency("UZS", "Uzbek Som", "сўм", "🇺🇿"),
        Currency("GEL", "Georgian Lari", "₾", "🇬🇪"),
        Currency("AZN", "Azerbaijani Manat", "₼", "🇦🇿"),
        Currency("AMD", "Armenian Dram", "֏", "🇦🇲"),
        Currency("NGN", "Nigerian Naira", "₦", "🇳🇬"),
        Currency("KES", "Kenyan Shilling", "KSh", "🇰🇪"),
        Currency("GHS", "Ghanaian Cedi", "₵", "🇬🇭"),
        Currency("TZS", "Tanzanian Shilling", "TSh", "🇹🇿"),
        Currency("UGX", "Ugandan Shilling", "USh", "🇺🇬"),
        Currency("ETB", "Ethiopian Birr", "Br", "🇪🇹"),
        Currency("MAD", "Moroccan Dirham", "د.م", "🇲🇦"),
        Currency("DZD", "Algerian Dinar", "د.ج", "🇩🇿"),
        Currency("TND", "Tunisian Dinar", "د.ت", "🇹🇳"),
        Currency("ARS", "Argentine Peso", "$", "🇦🇷"),
        Currency("CLP", "Chilean Peso", "$", "🇨🇱"),
        Currency("COP", "Colombian Peso", "$", "🇨🇴"),
        Currency("PEN", "Peruvian Sol", "S/", "🇵🇪"),
        Currency("VES", "Venezuelan Bolivar", "Bs", "🇻🇪"),
        Currency("UYU", "Uruguayan Peso", "\$U", "🇺🇾"),
        Currency("BOB", "Bolivian Boliviano", "Bs", "🇧🇴"),
        Currency("PYG", "Paraguayan Guarani", "₲", "🇵🇾"),
        Currency("CRC", "Costa Rican Colon", "₡", "🇨🇷"),
        Currency("GTQ", "Guatemalan Quetzal", "Q", "🇬🇹"),
        Currency("HNL", "Honduran Lempira", "L", "🇭🇳"),
        Currency("NIO", "Nicaraguan Cordoba", "C$", "🇳🇮"),
        Currency("PAB", "Panamanian Balboa", "B/.", "🇵🇦"),
        Currency("DOP", "Dominican Peso", "RD$", "🇩🇴"),
        Currency("JMD", "Jamaican Dollar", "J$", "🇯🇲"),
        Currency("TTD", "Trinidad Dollar", "TT$", "🇹🇹"),
        Currency("BBD", "Barbadian Dollar", "Bds$", "🇧🇧"),
        Currency("BSD", "Bahamian Dollar", "B$", "🇧🇸"),
        Currency("BZD", "Belize Dollar", "BZ$", "🇧🇿"),
        Currency("XCD", "East Caribbean Dollar", "EC$", "🇦🇬"),
        Currency("FJD", "Fijian Dollar", "FJ$", "🇫🇯"),
        Currency("XPF", "CFP Franc", "₣", "🇵🇫"),
        Currency("PGK", "Papua New Guinean Kina", "K", "🇵🇬"),
        Currency("WST", "Samoan Tala", "WS$", "🇼🇸"),
        Currency("TOP", "Tongan Paʻanga", "T$", "🇹🇴"),
        Currency("VUV", "Vanuatu Vatu", "VT", "🇻🇺"),
        Currency("SBD", "Solomon Islands Dollar", "SI$", "🇸🇧"),
        Currency("MOP", "Macanese Pataca", "MOP$", "🇲🇴"),
        Currency("BND", "Brunei Dollar", "B$", "🇧🇳"),
        Currency("AFN", "Afghan Afghani", "؋", "🇦🇫"),
        Currency("IQD", "Iraqi Dinar", "ع.د", "🇮🇶"),
        Currency("IRR", "Iranian Rial", "﷼", "🇮🇷"),
        Currency("JOD", "Jordanian Dinar", "د.ا", "🇯🇴"),
        Currency("LBP", "Lebanese Pound", "ل.ل", "🇱🇧"),
        Currency("SYP", "Syrian Pound", "£S", "🇸🇾"),
        Currency("YER", "Yemeni Rial", "﷼", "🇾🇪"),
        Currency("MNT", "Mongolian Tugrik", "₮", "🇲🇳"),
        Currency("KPW", "North Korean Won", "₩", "🇰🇵"),
        Currency("MVR", "Maldivian Rufiyaa", "Rf", "🇲🇻"),
        Currency("BTN", "Bhutanese Ngultrum", "Nu.", "🇧🇹"),
        Currency("XOF", "West African CFA Franc", "CFA", "🇸🇳"),
        Currency("XAF", "Central African CFA Franc", "FCFA", "🇨🇲"),
        Currency("RWF", "Rwandan Franc", "FRw", "🇷🇼"),
        Currency("BIF", "Burundian Franc", "FBu", "🇧🇮"),
        Currency("CDF", "Congolese Franc", "FC", "🇨🇩"),
        Currency("MWK", "Malawian Kwacha", "MK", "🇲🇼"),
        Currency("ZMW", "Zambian Kwacha", "ZK", "🇿🇲"),
        Currency("ZWL", "Zimbabwean Dollar", "Z$", "🇿🇼"),
        Currency("BWP", "Botswana Pula", "P", "🇧🇼"),
        Currency("NAD", "Namibian Dollar", "N$", "🇳🇦"),
        Currency("SZL", "Swazi Lilangeni", "E", "🇸🇿"),
        Currency("LSL", "Lesotho Loti", "M", "🇱🇸"),
        Currency("MUR", "Mauritian Rupee", "₨", "🇲🇺"),
        Currency("SCR", "Seychellois Rupee", "₨", "🇸🇨"),
        Currency("MGA", "Malagasy Ariary", "Ar", "🇲🇬"),
        Currency("KMF", "Comorian Franc", "CF", "🇰🇲"),
        Currency("DJF", "Djiboutian Franc", "Fdj", "🇩🇯"),
        Currency("SOS", "Somali Shilling", "S", "🇸🇴"),
        Currency("ERN", "Eritrean Nakfa", "Nfk", "🇪🇷"),
        Currency("SDG", "Sudanese Pound", "ج.س", "🇸🇩"),
        Currency("SSP", "South Sudanese Pound", "£", "🇸🇸"),
        Currency("AOA", "Angolan Kwanza", "Kz", "🇦🇴"),
        Currency("MZN", "Mozambican Metical", "MT", "🇲🇿"),
        Currency("CVE", "Cape Verdean Escudo", "$", "🇨🇻"),
        Currency("GMD", "Gambian Dalasi", "D", "🇬🇲"),
        Currency("GNF", "Guinean Franc", "FG", "🇬🇳"),
        Currency("LRD", "Liberian Dollar", "L$", "🇱🇷"),
        Currency("SLL", "Sierra Leonean Leone", "Le", "🇸🇱"),
        Currency("STN", "São Tomé Dobra", "Db", "🇸🇹"),
        Currency("HTG", "Haitian Gourde", "G", "🇭🇹"),
        Currency("CUP", "Cuban Peso", "₱", "🇨🇺"),
        Currency("AWG", "Aruban Florin", "ƒ", "🇦🇼"),
        Currency("ANG", "Netherlands Antillean Guilder", "ƒ", "🇨🇼"),
        Currency("SRD", "Surinamese Dollar", "$", "🇸🇷"),
        Currency("GYD", "Guyanese Dollar", "G$", "🇬🇾"),
        Currency("FKP", "Falkland Islands Pound", "£", "🇫🇰"),
        Currency("ISK", "Icelandic Krona", "kr", "🇮🇸"),
        Currency("GIP", "Gibraltar Pound", "£", "🇬🇮"),
        Currency("BAM", "Bosnia-Herzegovina Mark", "KM", "🇧🇦"),
        Currency("MKD", "Macedonian Denar", "ден", "🇲🇰"),
        Currency("ALL", "Albanian Lek", "L", "🇦🇱"),
        Currency("MDL", "Moldovan Leu", "L", "🇲🇩"),
        Currency("TJS", "Tajikistani Somoni", "SM", "🇹🇯"),
        Currency("KGS", "Kyrgyzstani Som", "сом", "🇰🇬"),
        Currency("TMT", "Turkmenistani Manat", "T", "🇹🇲"),
        Currency("SHP", "Saint Helena Pound", "£", "🇸🇭"),
        Currency("JEP", "Jersey Pound", "£", "🇯🇪"),
        Currency("GGP", "Guernsey Pound", "£", "🇬🇬"),
        Currency("IMP", "Isle of Man Pound", "£", "🇮🇲"),
        // Cryptocurrencies
        Currency("BTC", "Bitcoin", "₿", "🪙"),
        Currency("ETH", "Ethereum", "Ξ", "🪙"),
        Currency("USDT", "Tether", "₮", "🪙")
    )
    
    /**
     * Get a currency by its code.
     */
    fun getCurrency(code: String): Currency? = currencies.find { it.code == code }
    
    /**
     * Get the symbol for a currency code.
     */
    fun getSymbol(code: String): String = getCurrency(code)?.symbol ?: "$"
    
    /**
     * Search currencies by name or code.
     */
    fun search(query: String): List<Currency> {
        val lowerQuery = query.lowercase()
        return currencies.filter {
            it.code.lowercase().contains(lowerQuery) ||
            it.name.lowercase().contains(lowerQuery)
        }
    }
}
