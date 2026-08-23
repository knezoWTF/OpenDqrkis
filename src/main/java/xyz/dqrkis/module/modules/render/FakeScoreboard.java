package xyz.dqrkis.module.modules.render;

import xyz.dqrkis.event.events.HudListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.StringSetting;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.client.gui.DrawContext;

public final class FakeScoreboard extends Module implements HudListener {
	private final StringSetting title = new StringSetting(EncryptedString.of("Title"), EncryptedString.of("Stats").toString());
	private final StringSetting money = new StringSetting(EncryptedString.of("Money"), EncryptedString.of("$14,320").toString());
	private final StringSetting shards = new StringSetting(EncryptedString.of("Shards"), EncryptedString.of("2.3K").toString());
	private final StringSetting kills = new StringSetting(EncryptedString.of("Kills"), EncryptedString.of("503").toString());
	private final StringSetting deaths = new StringSetting(EncryptedString.of("Deaths"), EncryptedString.of("421").toString());
	private final StringSetting keyAll = new StringSetting(EncryptedString.of("Key All"), EncryptedString.of("67m 67s").toString());
	private final StringSetting playtime = new StringSetting(EncryptedString.of("Playtime"), EncryptedString.of("22d 9h").toString());
	private final StringSetting team = new StringSetting(EncryptedString.of("Team"), EncryptedString.of("Elites").toString());
	private final StringSetting region = new StringSetting(EncryptedString.of("Region"), EncryptedString.of("Spawn").toString());
	private final StringSetting ping = new StringSetting(EncryptedString.of("Ping"), EncryptedString.of("24").toString());

	public FakeScoreboard() {
		super(EncryptedString.of("Fake Scoreboard"),
				EncryptedString.of("Shows a fake scoreboard."),
				-1,
				Category.RENDER);
		addSettings(title, money, shards, kills, deaths, keyAll, playtime, team, region, ping);
	}

	@Override
	public void onEnable() {
		eventManager.add(HudListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(HudListener.class, this);
		super.onDisable();
	}

	@Override
	public void onRenderHud(HudEvent event) {
		DrawContext context = event.context;
		var textRenderer = mc.textRenderer;

		String[][] rows = {
				{"Money", money.getValue()},
				{"Shards", shards.getValue()},
				{"Kills", kills.getValue()},
				{"Deaths", deaths.getValue()},
				{"Key All", keyAll.getValue()},
				{"Playtime", playtime.getValue()},
				{"Team", team.getValue()},
				{"Region", region.getValue()},
				{"Ping", ping.getValue()}
		};

		int width = textRenderer.getWidth(title.getValue());
		for (String[] row : rows)
			width = Math.max(width, textRenderer.getWidth(row[0] + ": " + row[1]));

		width += 8;
		int lineHeight = textRenderer.fontHeight + 2;
		int height = rows.length * lineHeight + 14;
		int x = context.getScaledWindowWidth() - width - 4;
		int y = 4;

		context.fill(x, y, x + width, y + height, 0x503000B0);
		context.fill(x, y, x + width, y + 1, 0xFF000000);
		context.fill(x, y + height - 1, x + width, y + height, 0xFF000000);
		context.fill(x, y, x + 1, y + height, 0xFF000000);
		context.fill(x + width - 1, y, x + width, y + height, 0xFF000000);

		context.drawText(textRenderer, title.getValue(), x + (width - textRenderer.getWidth(title.getValue())) / 2,
				y + 2, 0xFFFFFF55, false);

		for (int i = 0; i < rows.length; i++) {
			int rowY = y + 14 + i * lineHeight;
			context.drawText(textRenderer, rows[i][0] + ":", x + 4, rowY, 0xFFFFFFFF, false);
			String value = rows[i][1];
			context.drawText(textRenderer, value, x + width - 4 - textRenderer.getWidth(value), rowY, 0xFFFFFFFF, false);
		}
	}
}
