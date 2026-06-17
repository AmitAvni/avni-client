package com.avni.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives the main menu an Avni Client identity: a custom aurora background in
 * place of the panorama, an "AVNI CLIENT" wordmark instead of the Minecraft
 * logo, no splash text, and a small footer watermark.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    // aurora palette (ARGB)
    private static final int BG_TOP = 0xFF0E1320;
    private static final int BG_BOTTOM = 0xFF06080D;
    private static final int MINT = 0xFF43E6A8;
    private static final int ACCENT = 0xFF2FE0A6;

    private static final Identifier CUBE = Identifier.of("avni-client", "textures/gui/cube.png");
    private static final Identifier FONT = Identifier.of("avni-client", "avni");

    /** Replace the rotating panorama with a custom aurora background. */
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground"
                    + "(Lnet/minecraft/client/gui/DrawContext;F)V"))
    private void avni$background(TitleScreen instance, DrawContext ctx, float delta) {
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        // base wash
        ctx.fillGradient(0, 0, w, h, BG_TOP, BG_BOTTOM);

        // smooth aurora glows: each is a pair that fades 0 -> peak -> 0 (no hard seams)
        int cyanPeak = (int) (h * 0.42);
        ctx.fillGradient(0, 0, w, cyanPeak, 0x0033C9FF, 0x3833C9FF);
        ctx.fillGradient(0, cyanPeak, w, h, 0x3833C9FF, 0x0033C9FF);

        int mintPeak = (int) (h * 0.30);
        ctx.fillGradient(0, 0, w, mintPeak, 0x002FE0A6, 0x242FE0A6);
        ctx.fillGradient(0, mintPeak, w, h, 0x242FE0A6, 0x002FE0A6);

        int violetPeak = (int) (h * 0.52);
        ctx.fillGradient(0, 0, w, violetPeak, 0x008B7BFF, 0x1C8B7BFF);
        ctx.fillGradient(0, violetPeak, w, h, 0x1C8B7BFF, 0x008B7BFF);
    }

    /** Draw the Avni wordmark instead of the vanilla Minecraft logo. */
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"))
    private void avni$wordmark(LogoDrawer instance, DrawContext ctx, int screenWidth, float alpha) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int cx = ctx.getScaledWindowWidth() / 2;

        // voxel cube logo, centered above the wordmark
        int cube = 46;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, CUBE, cx - cube / 2, 14, 0.0F, 0.0F,
                cube, cube, cube, cube);

        // "AVNI CLIENT" in the launcher's Chakra Petch font
        Text title = Text.literal("AVNI CLIENT")
                .setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(FONT)));
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, 74);
        m.scale(2.4f, 2.4f);
        ctx.drawCenteredTextWithShadow(tr, title, 0, 0, MINT);
        m.popMatrix();
    }

    /** Hide the vanilla splash text. */
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/SplashTextRenderer;render"
                    + "(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/client/font/TextRenderer;F)V"))
    private void avni$noSplash(SplashTextRenderer instance, DrawContext ctx, int centerX,
                              TextRenderer textRenderer, float alpha) {
        // intentionally empty — Avni replaces the splash
    }

    /** Footer watermark, centered along the bottom (clear of the vanilla corner text). */
    @Inject(method = "render", at = @At("TAIL"))
    private void avni$footer(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String text = "Avni Client " + "1.0.0";
        int x = (ctx.getScaledWindowWidth() - tr.getWidth(text)) / 2;
        int y = ctx.getScaledWindowHeight() - tr.fontHeight - 5;
        ctx.drawTextWithShadow(tr, text, x, y, ACCENT);
    }
}
