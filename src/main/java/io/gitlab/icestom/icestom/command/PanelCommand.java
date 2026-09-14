package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.web.PanelServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class PanelCommand extends Command {

    private static final Logger log = LoggerFactory.getLogger(PanelCommand.class);

    public PanelCommand() {
        super("panel");

        setDefaultExecutor(this::execute);
    }

    private void execute(CommandSender sender, CommandContext context) {
        PanelServer panel = IceStom.getInstance().getPanelServer();

        if (panel == null) {
            sender.sendMessage(Component.translatable("command.panel.disabled"));
            return;
        }

        boolean fromConsole = !(sender instanceof Player);

        UUID uuid = sender instanceof Player player ? player.getUuid() : null;
        String name = sender instanceof Player player ? player.getUsername() : "console";

        boolean allowed = fromConsole
                || panel.isOperator(uuid, name)
                || (sender instanceof IceStomPlayer player && player.hasPermission("icestom.panel"));

        if (!allowed) {
            sender.sendMessage(Component.translatable("command.panel.not_allowed"));
            return;
        }

        String link = panel.createLink(uuid, name);

        if (fromConsole) {
            log.info("Event panel link for console (expires soon, do not share): {}", link);
            return;
        }

        log.info("Issued an event panel link to {}", name);

        sender.sendMessage(Component.translatable("command.panel.link",
                Argument.component("link", Component.text(link).clickEvent(ClickEvent.openUrl(link)))
        ));
    }
}
