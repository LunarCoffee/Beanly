package dev.lunarcoffee.beanly.exts.commands.oyster

import java.io.Serializable

class OysterCatch(val name: String, val description: String) : Serializable {
    companion object {
        val EMPTY = OysterCatch("", "")
    }
}
