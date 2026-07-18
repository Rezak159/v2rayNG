package com.v2ray.ang.enums

enum class RoutingType(val fileName: String) {
    // a4vpn: the only preset offered in the UI (see R.array.preset_rulesets)
    ROSCOM("custom_routing_roscom"),
    WHITE("custom_routing_white"),
    BLACK("custom_routing_black"),
    GLOBAL("custom_routing_global"),
    WHITE_IRAN("custom_routing_white_iran"),
    WHITE_RUSSIA("custom_routing_white_russia");

    companion object {
        fun fromIndex(index: Int): RoutingType {
            return when (index) {
                0 -> ROSCOM
                1 -> WHITE
                2 -> BLACK
                3 -> GLOBAL
                4 -> WHITE_IRAN
                5 -> WHITE_RUSSIA
                else -> ROSCOM
            }
        }
    }
}
