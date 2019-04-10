package framework

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import org.reflections.Reflections
import org.yaml.snakeyaml.Yaml
import java.io.File


class Bot(configPath: String) {
    val config = Yaml().loadAs(File(configPath).readText(), BotConfig::class.java)!!

    private val jda: JDA = JDABuilder()
        .setToken(config.token)
        .build()

    private val dispatcher = Dispatcher(jda, this, config.prefix)
    private val reflections = Reflections()

    private val commandGroups = reflections.getTypesAnnotatedWith(CommandGroup::class.java)

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

    var activity: Activity?
        get() = jda.presence.activity
        set(value) {
            jda.presence.activity = value
        }

    fun start() = loadCommands()

    private fun loadCommands() {
        groupToCommands
            .values
            .flatten()
            .forEach { dispatcher.addCommand(it) }

        dispatcher.registerCommands()
    }
}
