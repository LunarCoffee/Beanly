package framework

import framework.annotations.CommandGroup
import net.dv8tion.jda.api.JDABuilder
import org.yaml.snakeyaml.Yaml
import java.io.File

open class Bot(configPath: String) {
    val config = Yaml()
        .loadAs(File(configPath).readText(), BotConfig::class.java)!!
        .also { currentConfig = it }

    val jda = JDABuilder()
        .setToken(config.token)
        .build()!!

    @Suppress("LeakingThis")
    private val dispatcher = Dispatcher(jda, this, config.prefix)
    private val classLoader = ClassLoader.getSystemClassLoader()

    // Classes that contain commands. Would've done this with reflection and annotation checking,
    // but the Kotlin compiler plugin I use for the code execution commands conflicts with
    // Reflections (only library that worked), so I had to do this.
    private val commandGroups = File("src/main/kotlin/beanly")
        .walk()
        .filter { it.name.endsWith("Commands.kt") }
        .map { classLoader.loadClass("beanly.exts.${it.nameWithoutExtension}") }

    // Map of [CommandGroup]s to their [Command]s with messy reflection stuff.
    val groupToCommands = commandGroups
        .map { group -> group.methods.filter { it.returnType == BaseCommand::class.java } }
        .zip(commandGroups.map { it.newInstance() })
        .map { (methods, group) ->
            group::class.annotations.find { it is CommandGroup } as CommandGroup to methods.map {
                it.invoke(group) as BaseCommand
            }
        }
        .toMap()

    val commands = groupToCommands.values.flatten()

    init {
        // Register all classes marked with the [ListenerGroup] annotation as event listeners.
        // Most, if not all of this complexity, is simply checking for the correct types and
        // constructor signatures to prevent mistakes. And very painful reflection.
        jda.addEventListener(
            *File("src/main/kotlin/beanly")
                .walk()
                .filter { it.name.endsWith("Listener.kt") }
                .map { classFile ->
                    classLoader
                        .loadClass("beanly.exts.${classFile.nameWithoutExtension}")
                        .constructors
                        .find {
                            // Make sure the constructor takes one argument of type [Bot].
                            it.parameters.run {
                                size == 1 && get(0).type.name == Bot::class.java.name
                            }
                        }!!.newInstance(this)
                }
                .toList()
                .toTypedArray()
        )
    }

    fun loadCommands() {
        groupToCommands
            .values
            .flatten()
            .forEach { dispatcher.addCommand(it) }

        dispatcher.registerCommands()
    }

    companion object {
        // By way of [Bot.currentConfig], this is only read from, never modified.
        lateinit var currentConfig: BotConfig
    }
}
