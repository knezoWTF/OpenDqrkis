package dev.lvstrng.argon.font;

import dev.lvstrng.argon.utils.EncryptedString;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.util.Random;

/**
 * @author superblaubeere27
 * @ported sprayD
 */
public final class GlyphPageFontRenderer {

	private static final char SECTION_SIGN = '\u00A7';

	public Random fontRandom = new Random();

	/**
	 * Current X coordinate at which to draw the next character.
	 */
	private float posX;
	/**
	 * Current Y coordinate at which to draw the next character.
	 */
	private float posY;
	/**
	 * Array of RGB triplets defining the 16 standard chat colors followed by 16
	 * darker version of the same colors for drop shadows.
	 */
	private final int[] colorCode = new int[32];
	/**
	 * Set if the "l" style (bold) is active in currently rendering string
	 */
	private boolean boldStyle;
	/**
	 * Set if the "o" style (italic) is active in currently rendering string
	 */
	private boolean italicStyle;
	/**
	 * Set if the "n" style (underlined) is active in currently rendering string
	 */
	private boolean underlineStyle;
	/**
	 * Set if the "m" style (strikethrough) is active in currently rendering string
	 */
	private boolean strikethroughStyle;

	private final GlyphPage regularGlyphPage;
	private final GlyphPage boldGlyphPage;
	private final GlyphPage italicGlyphPage;
	private final GlyphPage boldItalicGlyphPage;


	public GlyphPageFontRenderer(GlyphPage regularGlyphPage, GlyphPage boldGlyphPage, GlyphPage italicGlyphPage,
								 GlyphPage boldItalicGlyphPage) {
		this.regularGlyphPage = regularGlyphPage;
		this.boldGlyphPage = boldGlyphPage;
		this.italicGlyphPage = italicGlyphPage;
		this.boldItalicGlyphPage = boldItalicGlyphPage;

		for (int i = 0; i < 32; ++i) {
			int j = (i >> 3 & 1) * 85;
			int k = (i >> 2 & 1) * 170 + j;
			int l = (i >> 1 & 1) * 170 + j;
			int i1 = (i & 1) * 170 + j;

			if (i == 6) {
				k += 85;
			}

			if (i >= 16) {
				k /= 4;
				l /= 4;
				i1 /= 4;
			}

			this.colorCode[i] = (k & 255) << 16 | (l & 255) << 8 | i1 & 255;
		}
	}

	public static GlyphPageFontRenderer create(CharSequence fontName, int size, boolean bold, boolean italic,
											   boolean boldItalic) {
		char[] chars = new char[256];

		for (int i = 0; i < chars.length; i++) {
			chars[i] = (char) i;
		}

		GlyphPage regularPage;

		regularPage = new GlyphPage(new Font(fontName.toString(), Font.PLAIN, size), true, true);

		regularPage.generateGlyphPage(chars);
		regularPage.setupTexture();

		GlyphPage boldPage = regularPage;
		GlyphPage italicPage = regularPage;
		GlyphPage boldItalicPage = regularPage;

		if (bold) {
			boldPage = new GlyphPage(new Font(fontName.toString(), Font.BOLD, size), true, true);

			boldPage.generateGlyphPage(chars);
			boldPage.setupTexture();
		}

		if (italic) {
			italicPage = new GlyphPage(new Font(fontName.toString(), Font.ITALIC, size), true, true);

			italicPage.generateGlyphPage(chars);
			italicPage.setupTexture();
		}

		if (boldItalic) {
			boldItalicPage = new GlyphPage(new Font(fontName.toString(), Font.BOLD | Font.ITALIC, size), true, true);

			boldItalicPage.generateGlyphPage(chars);
			boldItalicPage.setupTexture();
		}

		return new GlyphPageFontRenderer(regularPage, boldPage, italicPage, boldItalicPage);
	}

	public static GlyphPageFontRenderer createFromID(CharSequence id, int size, boolean bold, boolean italic,
													 boolean boldItalic) {
		char[] chars = new char[256];

		for (int i = 0; i < chars.length; i++) {
			chars[i] = (char) i;
		}

		Font font = null;

		try {
			font = Font.createFont(Font.TRUETYPE_FONT, GlyphPageFontRenderer.class.getResourceAsStream(id.toString()))
					.deriveFont(Font.PLAIN, size);
		} catch (Exception e) {
			e.printStackTrace();
		}

		GlyphPage regularPage;

		regularPage = new GlyphPage(font, true, true);
		regularPage.generateGlyphPage(chars);
		regularPage.setupTexture();

		GlyphPage boldPage = regularPage;
		GlyphPage italicPage = regularPage;
		GlyphPage boldItalicPage = regularPage;

		try {
			if (bold) {
				boldPage = new GlyphPage(
						Font.createFont(Font.TRUETYPE_FONT, GlyphPageFontRenderer.class.getResourceAsStream(id.toString()))
								.deriveFont(Font.BOLD, size),
						true, true);

				boldPage.generateGlyphPage(chars);
				boldPage.setupTexture();
			}

			if (italic) {
				italicPage = new GlyphPage(
						Font.createFont(Font.TRUETYPE_FONT, GlyphPageFontRenderer.class.getResourceAsStream(id.toString()))
								.deriveFont(Font.ITALIC, size),
						true, true);

				italicPage.generateGlyphPage(chars);
				italicPage.setupTexture();
			}

			if (boldItalic) {
				boldItalicPage = new GlyphPage(
						Font.createFont(Font.TRUETYPE_FONT, GlyphPageFontRenderer.class.getResourceAsStream(id.toString()))
								.deriveFont(Font.BOLD | Font.ITALIC, size),
						true, true);

				boldItalicPage.generateGlyphPage(chars);
				boldItalicPage.setupTexture();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new GlyphPageFontRenderer(regularPage, boldPage, italicPage, boldItalicPage);
	}

	public int drawStringWithShadow(DrawContext context, CharSequence text, float x, float y, int color) {
		return drawString(context, text, x, y, color, true);
	}

	public int drawStringWithShadow(DrawContext context, CharSequence text, double x, double y, int color) {
		return drawString(context, text, (float) x, (float) y, color, true);
	}

	public int drawString(DrawContext context, CharSequence text, float x, float y, int color) {
		return drawString(context, text, x, y, color, false);
	}

	public int drawString(DrawContext context, CharSequence text, double x, double y, int color) {
		return drawString(context, text, (float) x, (float) y, color, false);
	}

	public int drawCenteredString(DrawContext context, CharSequence text, double x, double y, float scale, int color) {
		return drawString(context, text, (float) x - getStringWidth(text) / 2, (float) y, scale, color, false);
	}

	public int drawCenteredString(DrawContext context, CharSequence text, double x, double y, int color) {
		return drawString(context, text, (float) x - getStringWidth(text) / 2, (float) y, color, false);
	}

	public int drawCenteredStringWidthShadow(DrawContext context, CharSequence text, double x, double y, int color) {
		return drawString(context, text, (float) x - getStringWidth(text) / 2, (float) y, color, true);
	}

	public int drawString(DrawContext context, CharSequence text, float x, float y, float scale, int color, boolean dropShadow) {
		this.resetStyles();
		int i;

		if (dropShadow) {
			i = this.renderString(context, text, x + 1.0F, y + 1.0F, scale, color, true);
			i = Math.max(i, this.renderString(context, text, x, y, scale, color, false));
		} else {
			i = this.renderString(context, text, x, y, scale, color, false);
		}

		return i;
	}

	/**
	 * Draws the specified string.
	 */
	public int drawString(DrawContext context, CharSequence text, float x, float y, int color, boolean dropShadow) {
		this.resetStyles();
		int i;

		if (dropShadow) {
			i = this.renderString(context, text, x + 1.0F, y + 1.0F, color, true);
			i = Math.max(i, this.renderString(context, text, x, y, color, false));
		} else {
			i = this.renderString(context, text, x, y, color, false);
		}

		return i;
	}

	/**
	 * Render single line string by setting color, current (posX,posY), and
	 * calling renderStringAtPos()
	 */
	private int renderString(DrawContext context, CharSequence text, float x, float y, int color, boolean dropShadow) {
		if (text == null) {
			return 0;
		} else {

			if ((color & -67108864) == 0) {
				color |= -16777216;
			}

			if (dropShadow) {
				color = (color & 16579836) >> 2 | color & -16777216;
			}
			this.posX = x * 2.0f;
			this.posY = y * 2.0f;
			this.renderStringAtPos(context, text, dropShadow, color);
			return (int) (this.posX / 4.0f);
		}
	}

	private int renderString(DrawContext context, CharSequence text, float x, float y, float scale, int color, boolean dropShadow) {
		if (text == null) {
			return 0;
		} else {

			if ((color & -67108864) == 0) {
				color |= -16777216;
			}

			if (dropShadow) {
				color = (color & 16579836) >> 2 | color & -16777216;
			}
			this.posX = x * 2.0f;
			this.posY = y * 2.0f;
			this.renderStringAtPos(context, text, scale, dropShadow, color);
			return (int) (this.posX / 4.0f);
		}
	}

	/**
	 * Render a single line string at the current (posX,posY) and update posX
	 */
	private void renderStringAtPos(DrawContext context, CharSequence text, boolean shadow, int color) {
		GlyphPage glyphPage = getCurrentGlyphPage();
		float alpha = (float) (color >> 24 & 255) / 255.0F;
		float red = (float) (color >> 16 & 255) / 255.0F;
		float green = (float) (color >> 8 & 255) / 255.0F;
		float blue = (float) (color & 255) / 255.0F;

		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.scale(0.5F, 0.5F);

		for (int i = 0; i < text.length(); ++i) {
			char c0 = text.charAt(i);

			if (c0 == SECTION_SIGN && i + 1 < text.length()) {
				int i1 = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));

				if (i1 < 16) {
					this.boldStyle = false;
					this.strikethroughStyle = false;
					this.underlineStyle = false;
					this.italicStyle = false;

					if (i1 < 0) {
						i1 = 15;
					}

					if (shadow) {
						i1 += 16;
					}

					int j1 = this.colorCode[i1];

					red = (float) (j1 >> 16 & 255) / 255.0F;
					green = (float) (j1 >> 8 & 255) / 255.0F;
					blue = (float) (j1 & 255) / 255.0F;
				} else if (i1 == 16) {
				} else if (i1 == 17) {
					this.boldStyle = true;
				} else if (i1 == 18) {
					this.strikethroughStyle = true;
				} else if (i1 == 19) {
					this.underlineStyle = true;
				} else if (i1 == 20) {
					this.italicStyle = true;
				} else {
					this.boldStyle = false;
					this.strikethroughStyle = false;
					this.underlineStyle = false;
					this.italicStyle = false;
				}

				++i;
			} else {
				glyphPage = getCurrentGlyphPage();

				int currentColor = toArgb(red, green, blue, alpha);
				float f = glyphPage.drawChar(context, c0, posX, posY, currentColor);

				doDraw(context, f, glyphPage, currentColor);
			}
		}

		matrices.popMatrix();
	}

	private void renderStringAtPos(DrawContext context, CharSequence text, float scale, boolean shadow, int color) {
		GlyphPage glyphPage = getCurrentGlyphPage();
		float alpha = (float) (color >> 24 & 255) / 255.0F;
		float red = (float) (color >> 16 & 255) / 255.0F;
		float green = (float) (color >> 8 & 255) / 255.0F;
		float blue = (float) (color & 255) / 255.0F;

		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.scale(scale, scale);

		for (int i = 0; i < text.length(); ++i) {
			char c0 = text.charAt(i);

			if (c0 == SECTION_SIGN && i + 1 < text.length()) {
				int i1 = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));

				if (i1 < 16) {
					this.boldStyle = false;
					this.strikethroughStyle = false;
					this.underlineStyle = false;
					this.italicStyle = false;

					if (i1 < 0) {
						i1 = 15;
					}

					if (shadow) {
						i1 += 16;
					}

					int j1 = this.colorCode[i1];

					red = (float) (j1 >> 16 & 255) / 255.0F;
					green = (float) (j1 >> 8 & 255) / 255.0F;
					blue = (float) (j1 & 255) / 255.0F;
				} else if (i1 == 16) {
				} else if (i1 == 17) {
					this.boldStyle = true;
				} else if (i1 == 18) {
					this.strikethroughStyle = true;
				} else if (i1 == 19) {
					this.underlineStyle = true;
				} else if (i1 == 20) {
					this.italicStyle = true;
				} else {
					this.boldStyle = false;
					this.strikethroughStyle = false;
					this.underlineStyle = false;
					this.italicStyle = false;
				}

				++i;
			} else {
				glyphPage = getCurrentGlyphPage();

				int currentColor = toArgb(red, green, blue, alpha);
				float f = glyphPage.drawChar(context, c0, posX, posY, currentColor);

				doDraw(context, f, glyphPage, currentColor);
			}
		}

		matrices.popMatrix();
	}

	private void doDraw(DrawContext context, float f, GlyphPage glyphPage, int color) {
		if (this.strikethroughStyle) {
			drawLine(context, this.posX, this.posX + f, this.posY + (glyphPage.getMaxFontHeight() / 2.0F), color);
		}

		if (this.underlineStyle) {
			drawLine(context, this.posX - 1.0F, this.posX + f, this.posY + glyphPage.getMaxFontHeight(), color);
		}

		this.posX += f;
	}

	private void drawLine(DrawContext context, float startX, float endX, float y, int color) {
		int left = Math.round(Math.min(startX, endX));
		int right = Math.max(left + 1, Math.round(Math.max(startX, endX)));
		int top = Math.round(y - 1.0F);
		context.fill(left, top, right, top + 1, color);
	}

	private int toArgb(float red, float green, float blue, float alpha) {
		int packedAlpha = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
		int packedRed = Math.max(0, Math.min(255, Math.round(red * 255.0F)));
		int packedGreen = Math.max(0, Math.min(255, Math.round(green * 255.0F)));
		int packedBlue = Math.max(0, Math.min(255, Math.round(blue * 255.0F)));
		return packedAlpha << 24 | packedRed << 16 | packedGreen << 8 | packedBlue;
	}

	private GlyphPage getCurrentGlyphPage() {
		if (boldStyle && italicStyle)
			return boldItalicGlyphPage;
		else if (boldStyle)
			return boldGlyphPage;
		else if (italicStyle)
			return italicGlyphPage;
		else
			return regularGlyphPage;
	}

	/**
	 * Reset all style flag fields in the class to false; called at the start of
	 * string rendering
	 */
	private void resetStyles() {
		this.boldStyle = false;
		this.italicStyle = false;
		this.underlineStyle = false;
		this.strikethroughStyle = false;
	}

	public int getFontHeight() {
		return regularGlyphPage.getMaxFontHeight() / 2;
	}

	public int getStringWidth(CharSequence text) {
		if (text == null) {
			return 0;
		}
		resetStyles();
		int width = 0;

		GlyphPage currentPage;

		int size = text.length();

		boolean on = false;

		for (int i = 0; i < size; i++) {
			char character = text.charAt(i);

			if (character == SECTION_SIGN)
				on = true;
			else if (on && character >= '0' && character <= 'r') {
				int colorIndex = "0123456789abcdefklmnor".indexOf(character);
				if (colorIndex < 16) {
					boldStyle = false;
					italicStyle = false;
				} else if (colorIndex == 17) {
					boldStyle = true;
				} else if (colorIndex == 20) {
					italicStyle = true;
				} else if (colorIndex == 21) {
					boldStyle = false;
					italicStyle = false;
				}
				i++;
				on = false;
			} else {
				if (on)
					i--;

				character = text.charAt(i);

				currentPage = getCurrentGlyphPage();

				width += currentPage.getWidth(character) - 8;
			}
		}

		return width / 2;
	}

	/**
	 * Trims a string to fit a specified Width.
	 */
	public CharSequence trimStringToWidth(CharSequence text, int width) {
		return this.trimStringToWidth(text, width, false);
	}

	/**
	 * Trims a string to a specified width, and will reverse it if par3 is set.
	 */
	public CharSequence trimStringToWidth(CharSequence text, int maxWidth, boolean reverse) {
		StringBuilder stringbuilder = new StringBuilder();
		resetStyles();

		boolean on = false;

		int j = reverse ? text.length() - 1 : 0;
		int k = reverse ? -1 : 1;
		int width = 0;

		GlyphPage currentPage;

		for (int i = j; i >= 0 && i < text.length() && i < maxWidth; i += k) {
			char character = text.charAt(i);

			if (character == SECTION_SIGN)
				on = true;
			else if (on && character >= '0' && character <= 'r') {
				int colorIndex = "0123456789abcdefklmnor".indexOf(character);
				if (colorIndex < 16) {
					boldStyle = false;
					italicStyle = false;
				} else if (colorIndex == 17) {
					boldStyle = true;
				} else if (colorIndex == 20) {
					italicStyle = true;
				} else if (colorIndex == 21) {
					boldStyle = false;
					italicStyle = false;
				}
				i++;
				on = false;
			} else {
				if (on)
					i--;

				character = text.charAt(i);

				currentPage = getCurrentGlyphPage();

				width += (currentPage.getWidth(character) - 8) / 2;
			}

			if (i > width) {
				break;
			}

			if (reverse) {
				stringbuilder.insert(0, character);
			} else {
				stringbuilder.append(character);
			}
		}

		return EncryptedString.of(stringbuilder.toString());
	}
}
