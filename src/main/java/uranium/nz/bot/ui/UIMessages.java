package uranium.nz.bot.ui;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class UIMessages {

    public static MessageCreateData root() {
        return new MessageCreateBuilder()
                .setContent("🧩 **Whitelist Manager**")
                .setComponents(
                        ActionRow.of(
                                Button.secondary("wl:add", "➕"),
                                Button.secondary("wl:remove", "➖")
                        ),
                        ActionRow.of(
                                Button.secondary("wl:find", "🔍"),
                                Button.secondary("wl:change", "✏️")
                        ),
                        ActionRow.of(
                                Button.secondary("wl:prev", "⬅️"),
                                Button.secondary("wl:close", "❌")
                        )
                )
                .build();
    }
    public static MessageCreateData addUser() {
        return createUserSelectMenu("Select a user to add to the whitelist");
    }
    public static MessageCreateData removeUser() {
        return createUserSelectMenu("Select a user to remove from the whitelist");
    }
    public static MessageCreateData findUser() {
        return createUserSelectMenu("Select a user to find in the whitelist");
    }
    public static MessageCreateData changeUser() {
        return createUserSelectMenu("Select a user to change in the whitelist");
    }

    public static MessageCreateData showAddUserOptions(Member member, boolean isExistingUser) {
        String content = String.format("Ви вибрали %s., якщо все вірно то можете продовжувати", member.getAsMention());
        Button actionButton = isExistingUser
                ? Button.success("wl:add_twin", "➕ Add Twin")
                : Button.success("wl:add_main", "➕ Add Main ");

        return new MessageCreateBuilder()
                .setContent(content)
                .setComponents(
                        ActionRow.of(actionButton),
                        ActionRow.of(
                                Button.primary("wl:prev", "⬅️"),
                                Button.danger("wl:close", "❌")
                        )
                ).build();
    }

    private static MessageCreateData createUserSelectMenu(String placeholder) {
        return new MessageCreateBuilder()
            .setContent("🧩 **Whitelist Manager**")
            .setComponents(
                ActionRow.of(
                    EntitySelectMenu.create("wl:user", EntitySelectMenu.SelectTarget.USER)
                        .setPlaceholder(placeholder)
                        .build()
                ),
                ActionRow.of(
                    Button.primary("wl:prev", "⬅️"),
                    Button.danger("wl:close", "❌")
                )
            ).build();
    }
}
