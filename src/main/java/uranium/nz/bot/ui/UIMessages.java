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
                .setContent("🧩 **Менеджер Вайтлисту**")
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
        return createUserSelectMenu("Виберіть користувача, щоб додати до вайтлисту");
    }
    public static MessageCreateData removeUser() {
        return createUserSelectMenu("Виберіть користувача, щоб видалити з вайтлисту");
    }
    public static MessageCreateData findUser() {
        return createUserSelectMenu("Виберіть користувача, щоб знайти у вайтлисті");
    }
    public static MessageCreateData changeUser() {
        return createUserSelectMenu("Виберіть користувача, щоб змінити у вайтлисті");
    }

    public static MessageCreateData showAddUserOptions(Member member, boolean isExistingUser) {
        String content = String.format("Ви вибрали %s. Якщо все вірно, можете продовжувати.", member.getAsMention());
        Button actionButton = isExistingUser
                ? Button.success("wl:add_twin", "➕ Додати твінк")
                : Button.success("wl:add_main", "➕ Додати основу");

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

    public static MessageCreateData showRemoveUserOptions(Member member, boolean hasMain, boolean hasTwins) {
        String content = String.format("Ви вибрали %s для видалення. Що саме ви хочете видалити?", member.getAsMention());
        
        ActionRow buttonRow;
        if (hasMain && hasTwins) {
            buttonRow = ActionRow.of(
                    Button.danger("wl:remove_main", "➕ Видалити основу"),
                    Button.danger("wl:remove_twin", "➕ Видалити твінк")
            );
        } else if (hasMain) {
            buttonRow = ActionRow.of(Button.danger("wl:remove_main", "Видалити основу"));
        } else if (hasTwins) {
            buttonRow = ActionRow.of(Button.danger("wl:remove_twin", "Видалити твінк"));
        } else {
            content = String.format("Для користувача %s не знайдено записів у вайтлисті.", member.getAsMention());
            buttonRow = ActionRow.of(Button.primary("wl:prev", "⬅️").asDisabled()); // No action to take
        }

        return new MessageCreateBuilder()
                .setContent(content)
                .setComponents(buttonRow, ActionRow.of(Button.primary("wl:prev", "⬅️"), Button.danger("wl:close", "❌"))).build();
    }

    public static MessageCreateData promptForMainUsername(Member member) {
        String content = String.format("Ви додаєте основний акаунт для %s.\n\nБудь ласка, використовуйте команду `/whitelist add <ігровий_нік>` для завершення.", member.getAsMention());
        return new MessageCreateBuilder()
                .setContent(content)
                .setComponents(ActionRow.of(
                        Button.primary("wl:prev", "⬅️"),
                        Button.danger("wl:close", "❌")))
                .build();
    }

    public static MessageCreateData promptForTwinUsername(Member member) {
        String content = String.format("Ви додаєте твінк акаунт для %s.\n\nБудь ласка, використовуйте команду `/whitelist add <ігровий_нік>` для завершення.", member.getAsMention());
        return new MessageCreateBuilder()
                .setContent(content)
                .setComponents(ActionRow.of(
                        Button.primary("wl:prev", "⬅️"),
                        Button.danger("wl:close", "❌")))
                .build();
    }

    private static MessageCreateData createUserSelectMenu(String placeholder) {
        return new MessageCreateBuilder()
            .setContent("🧩 **Менеджер Вайтлисту**")
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
