@file:Suppress("MemberVisibilityCanBePrivate")

package framework

import framework.annotations.CommandGroup
import framework.annotations.ListenerGroup
import mu.KotlinLogging
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.yaml.snakeyaml.Yaml
import java.io.File

open class Bot(configPath: String) {
    val config = Yaml().loadAs(File(configPath).readText(), BotConfig::class.java)!!

    val jda = JDABuilder()
        .setToken(config.token)
        .build()!!

    // This is safe because [Dispatcher] does not use the instance of [Bot] being constructed in
    // its constructor, but only when it will actually be fully initialized.
    @Suppress("LeakingThis")
    private val dispatcher = Dispatcher(jda, this, config.prefix)

    private val cl = ClassLoader.getSystemClassLoader()

    // Classes that contain commands. Would've done this with a nice library (like Reflections),
    // but the Kotlin compiler plugin I use for the code execution commands conflicts with
    // Reflections (which worked), so I had to do this.
    private val commandGroups = File(config.sourceRootDir)
        .walk()
        .mapNotNull { silence { cl.loadClass("${config.commandPkg}.${it.nameWithoutExtension}") } }
        .filter { c -> c.annotations.any { it.annotationClass == CommandGroup::class } }

    // Map of [CommandGroup]s to their [BaseCommand]s with messy reflection stuff.
    private val groupToCommands = commandGroups
        .map { group -> group.methods.filter { it.returnType == BaseCommand::class.java } }
        .zip(commandGroups.map { it.newInstance() })
        .map { (methods, group) ->
            val annotation = group::class.annotations.find { it is CommandGroup } as CommandGroup
            annotation to methods.map {
                (it.invoke(group) as BaseCommand).apply {
                    groupName = annotation.name
                }
            }
        }
        .toMap()
        .also {
            log.info {
                val groupNames = it.keys.map { it.name }
                "Loaded command groups: $groupNames"
            }
        }

    // Mutable to allow for dynamic loading of commands.
    var commands = groupToCommands.values.flatten().toMutableList()
    val commandNames
        get() = commands.flatMap { it.names }

    // Register all classes marked with the [ListenerGroup] annotation as event listeners. Most, if
    // not all of this complexity, is simply checking for the correct types and constructor
    // signatures to prevent mistakes. And very painful reflection.
    private val listenerGroups = File(config.sourceRootDir)
        .walk()
        .mapNotNull {
            silence { cl.loadClass("${config.listenerPkg}.${it.nameWithoutExtension}") }
        }
        .filter { c -> c.annotations.any { it.annotationClass == ListenerGroup::class } }
        .map { c ->
            c.constructors
                .find {
                    // Make sure the constructor takes one argument of type [Bot].
                    it.parameters.run {
                        size == 1 && get(0).type.name == Bot::class.java.name
                    }
                }!!.newInstance(this) as ListenerAdapter
        }
        .onEach { jda.addEventListener(it) }
        .also {
            log.info {
                val groupNames = it.map { it.javaClass.name.substringAfterLast(".") }.toList()
                "Loaded listener groups: $groupNames"
            }
        }

    // Mutable to allow for dynamic loading of event listeners.
    val listeners = listenerGroups.toMutableList()
    val listenerNames
        get() = listeners.map { it.javaClass.name.substringAfterLast(".") }

    // This method adds a command dynamically, and is not meant to be used as a replacement for
    // annotating a class with [CommandGroup] and defining commands in it.
    fun addCommand(command: BaseCommand) {
        commands.add(command)
        dispatcher.apply {
            addCommand(command)
            registerCommands()
        }
    }

    // This method adds a listener dynamically, and is not meant to be used as a replacement for
    // annotating a class with a [ListenerGroup].
    fun addListener(listener: ListenerAdapter) {
        listeners += listener
        jda.addEventListener(listener)
    }

    fun loadCommands() {
        groupToCommands
            .values
            .flatten()
            .forEach { dispatcher.addCommand(it) }
        dispatcher.registerCommands()
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
