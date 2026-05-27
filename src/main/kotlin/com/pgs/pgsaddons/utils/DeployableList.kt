package com.pgs.pgsaddons.utils

object DeployableList {
    fun getDeployableRange(name: String): Array<IntArray>? {

        for (entry in DeployableList.DeployableRangeData) {

            val names = entry[0] as Array<String>
            val values = entry[1] as Array<IntArray>

            if (name in names) {
                return values
            }
        }

        return null
    }
    val DeployableRangeData: Array<out Array<out Any>>
        get() = arrayOf(

            arrayOf(
                arrayOf("Radiant","Manaflux"),
                arrayOf(intArrayOf(18,30))
                ),
            arrayOf(
                arrayOf("Overflux","Plasmaflux"),
                arrayOf(intArrayOf(20,60))
                ),
            arrayOf(
                arrayOf("Warning","Alert","SOS"),
                arrayOf(intArrayOf(40,180))
            ),
            arrayOf(
                arrayOf("Umbrella","Lantern","Will-o'-wisp","Totem of Corruption"),
                arrayOf(intArrayOf(30,300))
            ),
            arrayOf(
                arrayOf("Black Hole"),
                arrayOf(intArrayOf(30,180))
            )
            )
}