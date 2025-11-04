package uranium.nz.bot.ui;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import uranium.nz.bot.database.WhitelistManager;

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

    public static MessageCreateData showAddUserOptions(Member member) {
        boolean hasMain = WhitelistManager.hasMain(member.getIdLong());
        boolean hasTwin = WhitelistManager.hasTwin(member.getIdLong());

        String content = String.format("Ви вибрали %s. ", member.getAsMention());

        MessageCreateBuilder message = new MessageCreateBuilder();

        if (!hasMain) {
            content += "Цей користувач ще не має основного акаунту.";
            message.addComponents(ActionRow.of(Button.success("wl:add_main", "➕ Додати основу")));
        } else if (!hasTwin) {
            content += "Цей користувач вже має основний акаунт, але ще не має твінка.";
            message.addComponents(ActionRow.of(Button.success("wl:add_twin", "➕ Додати твінк")));
        } else {
            content += "Цей користувач вже має основний акаунт і твінк. Ви не можете додати більше.";
        }

        return message.setContent(content)
                .addComponents(ActionRow.of(
                        Button.secondary("wl:prev", "⬅️"),
                        Button.secondary("wl:close", "❌"))
                ).build();
    }

    public static MessageCreateData promptForMainUsername(Member member) {
        String content = String.format("Ви додаєте основний акаунт для %s.\n\nБудь ласка, використовуйте команду `/whitelist add:<ігровий_нік>` для завершення.", member.getAsMention());
        return new MessageCreateBuilder()
                .setContent(content)
                .setComponents(ActionRow.of(
                        Button.secondary("wl:prev", "⬅️"),
                        Button.secondary("wl:close", "❌"))).build();
    }

    public static MessageCreateData promptForTwinUsername(Member member) {
        String content = String.format("Ви додаєте твінк акаунт для %s.\n\nБудь ласка, використовуйте команду `/wl add <ігровий_нік>` для завершення.", member.getAsMention());
        return new MessageCreateBuilder()
                .setContent(content)
                .setComponents(ActionRow.of(
                        Button.secondary("wl:prev", "⬅️"),
                        Button.secondary("wl:close", "❌"))).build();
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
                    Button.secondary("wl:prev", "⬅️"),
                    Button.secondary("wl:close", "❌")
                )
            ).build();
    }
}
