package dev.lunarcoffee.framework.core

// Loaded using SnakeYAML, which requires a zero-arg constructor (thus the values must be defined
// outside of a constructor).
class BotConfig {
    lateinit var prefix: String
    lateinit var token: String
    lateinit var ownerId: String

    lateinit var sourceRootDir: String
    lateinit var commandP: String
    lateinit var listenerP: String

    lateinit var successFormat: String
    lateinit var errorFormat: String
}
