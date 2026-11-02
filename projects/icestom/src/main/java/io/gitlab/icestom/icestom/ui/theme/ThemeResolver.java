package io.gitlab.icestom.icestom.ui.theme;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ThemeResolver {
    public static TagResolver themeResolver(Theme theme) {
        return TagResolver.resolver("theme", (args, _) -> {
            final String field_name = args.popOr("field expected").value();

            try {
                Method method = theme.getClass().getMethod(field_name);

                TextColor textColor = (TextColor) method.invoke(theme);

                return Tag.styling(TextColor.color(textColor));
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                return Tag.styling(NamedTextColor.WHITE);
            }
        });
    }
}
