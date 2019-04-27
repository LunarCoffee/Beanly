package framework

// Loaded using SnakeYAML, which requires a zero-arg constructor (thus the values must be defined
// outside of a constructor).
class BotConfig {
    lateinit var prefix: String
    lateinit var token: String
    lateinit var ownerId: String

    lateinit var sourceRootDir: String
    lateinit var commandPkg: String
    lateinit var listenerPkg: String
}
