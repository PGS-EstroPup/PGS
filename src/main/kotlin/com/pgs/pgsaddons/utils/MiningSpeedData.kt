package com.pgs.pgsaddons.utils

object MiningSpeedData {
    fun getMiningData(name: String): Array<IntArray>? {

        for (entry in miningData) {

            val names = entry[0] as Array<String>
            val values = entry[1] as Array<IntArray>

            if (name in names) {
                return values
            }
        }

        return null
    }
    val miningData: Array<out Array<out Any>>
        get() = arrayOf(

            arrayOf(
                arrayOf("Ruby"),
                arrayOf(
                    intArrayOf(22, 3067),
                    intArrayOf(21, 3210),
                    intArrayOf(20, 3366),
                    intArrayOf(19, 3539),
                    intArrayOf(18, 3730),
                    intArrayOf(17, 3943),
                    intArrayOf(16, 4182),
                    intArrayOf(15, 4452),
                    intArrayOf(14, 4759),
                    intArrayOf(13, 5112),
                    intArrayOf(12, 5520),
                    intArrayOf(11, 6001),
                    intArrayOf(10, 6572),
                    intArrayOf(9, 7264),
                    intArrayOf(8, 8118),
                    intArrayOf(7, 9201),
                    intArrayOf(6, 10616),
                    intArrayOf(5, 12546),
                    intArrayOf(4, 15334)
                )
            ),

            arrayOf(
                arrayOf("Jade", "Amber", "Sapphire", "Amethyst"),
                arrayOf(
                    intArrayOf(22, 4000),
                    intArrayOf(21, 4187),
                    intArrayOf(20, 4391),
                    intArrayOf(19, 4616),
                    intArrayOf(18, 4865),
                    intArrayOf(17, 5143),
                    intArrayOf(16, 5455),
                    intArrayOf(15, 5807),
                    intArrayOf(14, 6207),
                    intArrayOf(13, 6667),
                    intArrayOf(12, 7200),
                    intArrayOf(11, 7827),
                    intArrayOf(10, 8572),
                    intArrayOf(9, 9474),
                    intArrayOf(8, 10589),
                    intArrayOf(7, 12001),
                    intArrayOf(6, 13847),
                    intArrayOf(5, 16364),
                    intArrayOf(4, 20000)
                )
            ),

            arrayOf(
                arrayOf("Topaz", "Opal"),
                arrayOf(
                    intArrayOf(22, 5067),
                    intArrayOf(21, 5303),
                    intArrayOf(20, 5561),
                    intArrayOf(19, 5847),
                    intArrayOf(18, 6163),
                    intArrayOf(17, 6515),
                    intArrayOf(16, 6910),
                    intArrayOf(15, 7355),
                    intArrayOf(14, 7863),
                    intArrayOf(13, 8445),
                    intArrayOf(12, 9120),
                    intArrayOf(11, 9914),
                    intArrayOf(10, 10858),
                    intArrayOf(9, 12001),
                    intArrayOf(8, 13412),
                    intArrayOf(7, 15201),
                    intArrayOf(6, 17539),
                    intArrayOf(5, 20728),
                    intArrayOf(4, 25334)
                )
            ),

            arrayOf(
                arrayOf("Jasper"),
                arrayOf(
                    intArrayOf(22, 6400),
                    intArrayOf(21, 6698),
                    intArrayOf(20, 7025),
                    intArrayOf(19, 7385),
                    intArrayOf(18, 7784),
                    intArrayOf(17, 8229),
                    intArrayOf(16, 8728),
                    intArrayOf(15, 9291),
                    intArrayOf(14, 9932),
                    intArrayOf(13, 10667),
                    intArrayOf(12, 11520),
                    intArrayOf(11, 12522),
                    intArrayOf(10, 13715),
                    intArrayOf(9, 15158),
                    intArrayOf(8, 16942),
                    intArrayOf(7, 19201),
                    intArrayOf(6, 22154),
                    intArrayOf(5, 26182),
                    intArrayOf(4, 32000)
                )
            ),

            arrayOf(
                arrayOf("Onyx", "Aquamarine", "Citrine", "Peridot"),
                arrayOf(
                    intArrayOf(24, 6368),
                    intArrayOf(23, 6639),
                    intArrayOf(22, 6934),
                    intArrayOf(21, 7256),
                    intArrayOf(20, 7610),
                    intArrayOf(19, 8001),
                    intArrayOf(18, 8433),
                    intArrayOf(17, 8915),
                    intArrayOf(16, 9455),
                    intArrayOf(15, 10065),
                    intArrayOf(11, 13566),
                    intArrayOf(10, 14858),
                    intArrayOf(9, 16422),
                    intArrayOf(8, 18353),
                    intArrayOf(7, 20801),
                    intArrayOf(6, 24000),
                    intArrayOf(5, 28364),
                    intArrayOf(4, 34667)
                )
            ),

            arrayOf(
                arrayOf("Gray Mithril"),
                arrayOf(
                    intArrayOf(5, 2728),
                    intArrayOf(4, 3334),
                    intArrayOf(0, 30000)
                )
            ),

            arrayOf(
                arrayOf("Prismarine Mithril"),
                arrayOf(
                    intArrayOf(8, 2824),
                    intArrayOf(7, 3201),
                    intArrayOf(6, 3693),
                    intArrayOf(5, 4364),
                    intArrayOf(4, 5334),
                    intArrayOf(0, 48000)
                )
            ),

            arrayOf(
                arrayOf("Blue Mithril"),
                arrayOf(
                    intArrayOf(12, 3600),
                    intArrayOf(11, 3914),
                    intArrayOf(10, 4286),
                    intArrayOf(9, 4737),
                    intArrayOf(8, 5295),
                    intArrayOf(7, 6001),
                    intArrayOf(6, 6924),
                    intArrayOf(5, 8182),
                    intArrayOf(4, 10000)
                )
            ),

            arrayOf(
                arrayOf("Glacite"),
                arrayOf(
                    intArrayOf(26, 6793),
                    intArrayOf(25, 7059),
                    intArrayOf(24, 7347),
                    intArrayOf(23, 7660),
                    intArrayOf(22, 8000),
                    intArrayOf(21, 8373),
                    intArrayOf(20, 8781),
                    intArrayOf(19, 9231),
                    intArrayOf(18, 9730),
                    intArrayOf(11, 15653),
                    intArrayOf(10,17143),
                    intArrayOf(9, 18948),
                    intArrayOf(8, 21177),
                    intArrayOf(7, 24001),
                    intArrayOf(6, 27693),
                    intArrayOf(5, 32728),
                    intArrayOf(4, 40000),
                )
            ),
            arrayOf(
                arrayOf("Tungsten","Umber"),
                arrayOf(
                    intArrayOf(24, 6589),
                    intArrayOf(23, 6858),
                    intArrayOf(22, 7149),
                    intArrayOf(21, 7814),
                    intArrayOf(20, 8196),
                    intArrayOf(19, 8616),
                    intArrayOf(18, 9082),
                    intArrayOf(17, 9601),
                    intArrayOf(9, 17685),
                    intArrayOf(8, 19765),
                    intArrayOf(7, 22401),
                    intArrayOf(6, 25847),
                    intArrayOf(5, 30546),
                    intArrayOf(4, 37334)
                )
            ),
            arrayOf(
                arrayOf("Pure Gold", "Pure Diamond", "Pure Iron", "Pure Lapis", "Pure Redstone", "Pure Emerald", "Pure Quartz", "Pure Coal"),
                arrayOf(
                    intArrayOf(6, 2770),
                    intArrayOf(5, 3201),
                    intArrayOf(4, 3202),
                    intArrayOf(0, 36000),
                )
            ),
            arrayOf(
                arrayOf("Obsidian"),
                arrayOf(
                    intArrayOf(4, 3334),
                    intArrayOf(0, 30000),
                )
            )
        )
}