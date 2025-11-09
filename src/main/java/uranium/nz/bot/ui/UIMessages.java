package uranium.nz.bot.ui;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import uranium.nz.bot.database.WhitelistManager;
import uranium.nz.bot.database.WhitelistedUser;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
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
                                Button.secondary("wl:prev", "⬅️").withDisabled(true),
                                Button.secondary("wl:close", "❌")
                        )
                )
                .build();
    }
    public static MessageCreateData addUser() {
        return createUserSelectMenu("Виберіть користувача, щоб додати до вайтлисту");
    }
    public static MessageCreateData removeUser() {
        String placeholder = "Виберіть користувача для видалення...";
        String content = "Виберіть користувача зі списку, щоб видалити його.\n\n" +
                         "Якщо користувача немає на сервері, використайте команду `/whitelist remove <ID>`, щоб видалити його за Discord ID.";
        return createUserSelectMenu(placeholder, content);
    }
    public static MessageCreateData findUser() {
        String placeholder = "Виберіть користувача зі списку...";
        String content = "Виберіть користувача зі списку нижче.\n\n" +
                         "Якщо користувача немає на сервері, використайте команду `/whitelist find <query>`, де `query` - це Discord ID або ігровий нік.";
        return createUserSelectMenu(placeholder, content);
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
            message.addComponents(ActionRow.of(Button.secondary("wl:add_main", "➕ Додати основу")));
        } else if (!hasTwin) {
            content += "Цей користувач вже має основний акаунт, але ще не має твінка.";
            message.addComponents(ActionRow.of(Button.secondary("wl:add_twin", "➕ Додати твінк")));
        } else {
            content += "Цей користувач вже має основний акаунт і твінк. Ви не можете додати більше.";
        }

        return message.setContent(content)
                .addComponents(ActionRow.of(
                        Button.secondary("wl:prev", "⬅️"),
                        Button.secondary("wl:close", "❌"))
                ).build();
    }

    public static MessageCreateData showRemoveUserOptions(Member member) {
        return showRemoveUserOptions(member, null);
    }

    public static MessageCreateData showRemoveUserOptions(Member member, String statusMessage) {
        boolean hasMain = WhitelistManager.hasMain(member.getIdLong());
        boolean hasTwin = WhitelistManager.hasTwin(member.getIdLong());

        String content = String.format("Ви вибрали %s. Що ви хочете видалити?", member.getAsMention());

        if (statusMessage != null && !statusMessage.isBlank()) {
            content = statusMessage + "\n\n" + content;
        }

        Button removeMain = Button.secondary("wl:remove_main", "🗑️ Видалити все").withDisabled(!hasMain);
        Button removeTwin = Button.secondary("wl:remove_twin", "➖ Видалити твінк").withDisabled(!hasTwin);

        return new MessageCreateBuilder()
                .setContent(content)
                .setComponents(
                        ActionRow.of(removeMain, removeTwin),
                        ActionRow.of(
                                Button.secondary("wl:prev", "⬅️"),
                                Button.secondary("wl:close", "❌"))
                ).build();
    }

    public static MessageCreateData promptForMainUsername(Member member) {
        String content = String.format("Ви додаєте основний акаунт для %s.\n\nБудь ласка, використовуйте команду `/whitelist add <ігровий_нік>` для завершення.", member.getAsMention());
        return new MessageCreateBuilder()
                .setContent(content)
                .setComponents(ActionRow.of(
                        Button.secondary("wl:prev", "⬅️"),
                        Button.secondary("wl:close", "❌")))
                .build();
    }

    public static MessageCreateData promptForTwinUsername(Member member) {
        String content = String.format("Ви додаєте твінк акаунт для %s.\n\nБудь ласка, використовуйте команду `/whitelist add <ігровий_нік>` для завершення.", member.getAsMention());
        return new MessageCreateBuilder()
                .setContent(content)
                .setComponents(ActionRow.of(
                        Button.secondary("wl:prev", "⬅️"),
                        Button.secondary("wl:close", "❌")))
                .build();
    }

    public static MessageCreateData promptForRemovalConfirmation(Member member, String removalType) {
        String item = "main".equals(removalType) ? "всі дані для" : "твінк акаунт";
        String content = String.format("⚠️ **Ви впевнені?**\n\nВи збираєтеся видалити %s користувача %s. Цю дію неможливо скасувати.", item, member.getAsMention());

        Button confirmButton = Button.danger("wl:confirm_remove", "Так, видалити");
        Button cancelButton = Button.secondary("wl:cancel_remove", "Ні, скасувати");

        return new MessageCreateBuilder()
                .setContent(content)
                .setComponents(
                        ActionRow.of(confirmButton, cancelButton)
                )
                .build();
    }

    public static MessageCreateData showSuccessAndGoBack(String message, String backButtonId, String closeButtonId) {
        return new MessageCreateBuilder()
                .setContent(message) 
                .setComponents(
                        ActionRow.of(
                                Button.secondary(backButtonId, "⬅️"),
                                Button.secondary(closeButtonId, "❌")
                        )
                ).build();
    }

    public static MessageCreateData showFindUserResult(User user, Optional<WhitelistedUser> userData) {
        return showFindUserResult(user, userData, true);
    }

    public static MessageCreateData showFindUserResult(User user, Optional<WhitelistedUser> userData, boolean withButtons) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🔍 Результати пошуку");
        embed.setThumbnail(user.getEffectiveAvatarUrl());

        if (userData.isPresent()) {
            WhitelistedUser whitelistedUser = userData.get();
            embed.setColor(0x4CAF50); // Green
            embed.setDescription("Інформація про користувача " + user.getAsMention());
            embed.addField("Основний нік", "`" + whitelistedUser.minecraftName() + "`", true);
            embed.addField("Твінк нік", whitelistedUser.twinName() != null ? "`" + whitelistedUser.twinName() + "`" : "_Немає_", true);
            embed.addField("Додано", whitelistedUser.addedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), false);
        } else {
            embed.setColor(0xF44336); // Red
            embed.setDescription("Користувач " + user.getAsMention() + " **не знайдений** у вайтлисті.");
        }

        MessageCreateBuilder builder = new MessageCreateBuilder().setEmbeds(embed.build());

        if (withButtons) {
            builder.setComponents(ActionRow.of(
                    Button.secondary("wl:prev", "⬅️"),
                    Button.secondary("wl:close", "❌")
            ));
        }
        return builder.build();
    }

    private static MessageCreateData createUserSelectMenu(String placeholder) {
        return createUserSelectMenu(placeholder, "🧩 **Менеджер Вайтлисту**");
    }

    private static MessageCreateData createUserSelectMenu(String placeholder, String content) {
        return new MessageCreateBuilder()
            .setContent(content)
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
