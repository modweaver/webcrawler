package org.ecorous.webcrawler.extensions

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.snowflake
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import org.ecorous.webcrawler.CaseType
import org.ecorous.webcrawler.Cases
import org.ecorous.webcrawler.Cases.delete
import org.ecorous.webcrawler.Cases.toColor
import org.ecorous.webcrawler.Cases.updateContent
import org.ecorous.webcrawler.SERVER_ID
import org.ecorous.webcrawler.Utils
import org.ecorous.webcrawler.Utils.getUsername

@OptIn(KordPreview::class)
class ModerationExtension : Extension() {
    override val name = ""

    override suspend fun setup() {
        publicSlashCommand(::KickArgs) {
            name = "kick"
            description = "Kick a user from the server"

            guild(SERVER_ID)  // Otherwise it'll take an hour to update
            requirePermission(Permission.KickMembers)
            requireBotPermissions(Permission.KickMembers)
            action {
                val channel = arguments.target.getDmChannel()
                channel.createEmbed {
                    title = "Kicked!"
                    description =
                        "${arguments.target.mention}, you have been kicked from `${guild?.fetchGuild()?.name}`\nYou can re-join with a new invite link, but any further issues will be punished with a ban."
                    footer {
                        text = "Moderator: ${user.asUser().tag} (${user.asUser().id})"
                        icon = user.asUser().avatar?.url
                    }
                    timestamp = Clock.System.now()
                    field {
                        name = "Reason"
                        value = arguments.reason
                        inline = false
                    }
                    color = DISCORD_RED
                }
                guild!!.fetchGuild().kick(arguments.target.id, arguments.reason)
                Utils.sendModLog(
                    guild!!.fetchGuild(),
                    user.fetchMember(guild!!.id),
                    arguments.target,
                    CaseType.KICK,
                    arguments.reason,
                    Clock.System.now(),
                    Cases.reportCase(CaseType.KICK, arguments.target.id, user.id, arguments.reason)
                )
                respond {
                    content = "Kicked ${arguments.target.mention}!"
                }
            }
        }

        publicSlashCommand(::BanArgs) {
            name = "ban"
            description = "Ban a user from the server"

            guild(SERVER_ID)
            requirePermission(Permission.BanMembers)
            requireBotPermissions(Permission.BanMembers)
            action {
                val channel = arguments.target.getDmChannel()
                channel.createEmbed {
                    title = "Banned!"
                    description =
                        "${arguments.target.mention}, you have been banned from `${guild?.fetchGuild()?.name}`!\nTo appeal, message `ecorous` (<@604653220341743618>)."
                    footer {
                        text = "Moderator: ${user.asUser().tag} (${user.asUser().id})"
                        icon = user.asUser().avatar?.url
                    }
                    timestamp = Clock.System.now()
                    field {
                        name = "Reason"
                        value = arguments.reason
                        inline = false
                    }
                    color = DISCORD_RED
                }
                guild?.fetchGuild()?.ban(arguments.target.id) {
                    reason = arguments.reason
                }
                Utils.sendModLog(
                    guild!!.fetchGuild(),
                    user.fetchMember(guild!!.id),
                    arguments.target,
                    CaseType.BAN,
                    arguments.reason,
                    Clock.System.now(),
                    Cases.reportCase(CaseType.BAN, arguments.target.id, user.id, arguments.reason)
                )
                respond {
                    content = "Banned ${arguments.target.mention} (${arguments.target.id})!"
                }
            }
        }

        publicSlashCommand(::ForceBanArgs) {
            name = "forceban"
            description = "Ban a user from the server without needing to be in the server"

            guild(SERVER_ID)
            requirePermission(Permission.BanMembers)
            requireBotPermissions(Permission.BanMembers)
            action {
                var shouldSendDmFailedSuccess = false
                val u = bot.kordRef.getUser(arguments.target)
                try {
                u?.let {
                    val channel = u.getDmChannelOrNull()
                    channel?.createEmbed {
                        title = "Banned!"
                        description =
                            "You have been banned from `${guild?.fetchGuild()?.name}`!\nTo appeal, message `ecorous` (<@604653220341743618>)."
                        footer {
                            text = "Moderator: ${user.asUser().tag} (${user.asUser().id})"
                            icon = user.asUser().avatar?.url
                        }
                        timestamp = Clock.System.now()
                        field {
                            name = "Reason"
                            value = arguments.reason
                            inline = false
                        }
                        color = DISCORD_RED
                    } ?: { shouldSendDmFailedSuccess = true }
                }
                } catch (e: Exception) {
                    shouldSendDmFailedSuccess = true
                }
                guild?.fetchGuild()?.ban(arguments.target) {
                    reason = arguments.reason
                }
                Utils.sendModLog(
                    guild!!.fetchGuild(),
                    user.fetchMember(guild!!.id),
                    null, // we don't have a user object, nor a member object
                    CaseType.BAN,
                    arguments.reason,
                    Clock.System.now(),
                    Cases.reportCase(CaseType.BAN, arguments.target, user.id, arguments.reason),
                    arguments.target // we send the user id instead of the user object
                )
                if (shouldSendDmFailedSuccess) {
                    respond {
                        content = "Banned <@${arguments.target}>! (DM failed)"
                    }
                } else {
                    respond {
                        content = "Banned <@${arguments.target}>!"
                    }

                }
            }
        }

        publicSlashCommand(::NoteArgs) {
            name = "note"
            description = "Add a moderation note to a user"
            requirePermission(Permission.ModerateMembers)
            action {
                Utils.sendModLog(
                    guild!!.fetchGuild(),
                    user.fetchMember(guild!!.id),
                    arguments.user,
                    CaseType.NOTE,
                    arguments.content,
                    Clock.System.now(),
                    Cases.reportCase(CaseType.NOTE, arguments.user.id, user.id, arguments.content)
                )
                respond {
                    content = "Added note to ${arguments.user.mention} (${arguments.user.id})"
                }
            }
        }

        publicSlashCommand {
            name = "case"
            description = "Manage cases"
            requirePermission(Permission.ModerateMembers)
            publicSubCommand(::GetCaseArgs) {
                name = "get"
                description = "Get information about a case"
                action {
                    val case = Cases.getCase(arguments.number)
                    val caseUser = this.guild!!.kord.getUser(case.userId)!!
                    val caseModerator = this.guild!!.kord.getUser(case.moderatorId)!!
                    respond {
                        embed {
                            title = "Case #${case.id}"
                            color = case.toColor()
                            field {
                                name = "Type"
                                value = case.type.name
                            }
                            field {
                                name = when (case.type) {
                                    CaseType.NOTE -> "Content"
                                    else -> "Reason"
                                }
                                value = case.content
                            }
                            field {
                                name = "User"
                                value = "${caseUser.getUsername()} (${caseUser.mention} - ${caseUser.id}"
                            }
                            footer {
                                icon = caseModerator.avatar?.url
                                text = "${caseModerator.getUsername()} (${caseModerator.id})"
                            }
                        }
                    }
                }
            }
            publicSubCommand(::DeleteCaseArgs) {
                name = "delete"
                description = "Delete a case"
                action {
                    val case = Cases.getCase(arguments.number)
                    case.delete()
                    respondEphemeral {
                        content = "Deleted case #${case.id}"
                    }
                }
            }
            publicSubCommand(::ModifyCaseArgs) {
                name = "modify"
                description = "Modify a case"
                action {
                    val case = Cases.getCase(arguments.number)
                    case.updateContent(arguments.newContent, user.fetchMember(guild!!.id))
                    respond {
                        content = "Updated case #${case.id}"
                    }
                }
            }
        }
    }

    inner class KickArgs : Arguments() {
        val target by user {
            name = "user"
            description = "Member to kick"
        }

        val reason by string {
            name = "reason"
            description = "The reason to kick this member"
            maxLength = 512
        }
    }

    inner class BanArgs : Arguments() {
        val target by user {
            name = "user"
            description = "Member to ban"
        }

        val reason by string {
            name = "reason"
            description = "The reason to ban this member"
            maxLength = 512
        }
    }

    inner class ForceBanArgs : Arguments() {
        val target by snowflake {
            name = "user"
            description = "User ID to ban"
        }

        val reason by string {
            name = "reason"
            description = "The reason to ban this member"
            maxLength = 512
        }
    }

    inner class NoteArgs : Arguments() {
        val user by user {
            name = "user"
            description = "The user to add a note to"
        }
        val content by string {
            name = "content"
            description = "The content of the note"
        }
    }

    inner class GetCaseArgs : Arguments() {
        val number by int {
            name = "number"
            description = "The number of the case to get"
        }
    }

    inner class DeleteCaseArgs : Arguments() {
        val number by int {
            name = "number"
            description = "The number of the case to delete"
        }
    }

    inner class ModifyCaseArgs : Arguments() {
        val number by int {
            name = "number"
            description = "The number of the case to modify"
        }
        val newContent by string {
            name = "new_content"
            description = "The new content of the case"
        }
    }
}