package framework

import beanly.consts.EMBED_COLOR
import beanly.consts.EMOJI_PAGE_FACING_UP
import beanly.ifEmptyToString
import framework.dsl.command
import framework.dsl.embed
import framework.extensions.error
import framework.extensions.send
import framework.transformers.TrWord
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import org.reflections.Reflections

class Bot(private val jda: JDA, prefix: String) {
    private val dispatcher = Dispatcher(jda, prefix)
    private val reflections = Reflections()

    private val commandGroups = reflections.getTypesAnnotatedWith(CommandGroup::class.java)

    // Map of [CommandGroup]s to their [Command]s with messy reflection stuff.
    private val groupToCommands = commandGroups
        .map { group -> group.methods.filter { it.returnType == BaseCommand::class.java } }
        .zip(commandGroups.map { it.newInstance() })
        .map { (methods, group) ->
            group::class.annotations.find { it is CommandGroup } as CommandGroup to methods.map {
                it.invoke(group) as BaseCommand
            }
        }
        .toMap()

    private val commands = groupToCommands.values.flatten()

    var activity: Activity?
        get() = jda.presence.activity
        set(value) {
            jda.presence.activity = value
        }

    fun start() {
        generateHelpCommand()
        loadCommands()
    }

    private fun loadCommands() {
        groupToCommands
            .values
            .flatten()
            .forEach { dispatcher.addCommand(it) }

        dispatcher.registerCommands()
    }

    private fun generateHelpCommand() {
        dispatcher.addCommand(
            command("help") {
                description = "Shows help text for all or a specific command."

                expectedArgs = listOf(TrWord(true, name = "command name"))
                execute { ctx, args ->
                    val commandName = args.get<String>(0)
                    var command = commands.find { commandName in it.aliases + it.name }

                    // Lazy way of making help for the help command (this) work.
                    if (commandName == "help") {
                        command = this
                    }

                    if (!commandName.isBlank() && command == null) {
                        ctx.error("I can't find that command!")
                        return@execute
                    }

                    ctx.send(
                        embed {
                            if (commandName.isBlank()) {
                                title = "$EMOJI_PAGE_FACING_UP  All commands:"

                                for ((group, commands) in groupToCommands) {
                                    val names = commands.map { it.name }
                                    description += "**${group.name}**: $names\n"
                                }
                            } else if (command != null) {
                                title = "$EMOJI_PAGE_FACING_UP  Info on **${command.name}**:"
                                description = """
                                    **Aliases**: ${command.aliases.ifEmptyToString()}
                                    **Description**: ${command.description}
                                    **Arguments**: ${command.expectedArgs.ifEmptyToString()}
                                """.trimIndent()
                            }
                            color = EMBED_COLOR
                        }
                    )
                }
            }
        )
    }
}
