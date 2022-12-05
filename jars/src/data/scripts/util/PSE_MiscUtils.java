package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PSE_MiscUtils {
    private static final float UIScaling = Global.getSettings().getScreenScaleMult();
    public static final Color GREENCOLOR;
    public static final Color BLUCOLOR;
    private static LazyFont.DrawableString TODRAW14;
    static {
        GREENCOLOR = Global.getSettings().getColor("textFriendColor");
        BLUCOLOR = Global.getSettings().getColor("textNeutralColor");

        try {
            LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
            TODRAW14 = fontdraw.createText();
        } catch (FontException ignored) {
        }
    }

    private static final Vector2f PERCENTBARVEC1 = new Vector2f(21f, 0f); // Just 21 pixel of width of difference.
    private static final Vector2f PERCENTBARVEC2 = new Vector2f(50f, 58f);

    /**
     * @param value value from 0 to 1
     * @param ratio value from 0 to 1, when value equals ratio, returned alpha will be at the maximum value
     * @param minAlpha value from 0 to 1, minimum alpha value
     * @param maxAlpha value from 0 to 1, maximum alpha value
     * @return value from 0 to 1, alpha multiplier
     */
    public static float getSmoothAlpha(float value, float ratio, float minAlpha, float maxAlpha) {
        float alpha;
        if (value > 1 - ratio) { //second
            //alpha = (1f - value) / ratio;
            alpha = (((minAlpha - maxAlpha) / (1f - ratio)) * value) + maxAlpha - (((minAlpha - maxAlpha) / (1f - ratio)) * ratio);
        } else { //first
            //alpha = value / (1f - ratio);
            alpha = (((maxAlpha - minAlpha) / (ratio)) * value) + minAlpha;
        }

        MathUtils.clamp(alpha, minAlpha, maxAlpha); //just in case

        return alpha;
    }

    /**
     * @param value value from 0 to 1
     * @param ratio value from 0 to 1, when value equals ratio, returned alpha will be at the maximum value
     * @param minAlpha value from 0 to 1, minimum alpha value
     * @param maxAlpha value from 0 to 1, maximum alpha value
     * @return value from 0 to 1, alpha multiplier
     */
    public static float getSqrtAlpha(float value, float ratio, float minAlpha, float maxAlpha) {
        float alpha;
        if (value < 1 - ratio) {
            alpha = (float) ((maxAlpha - minAlpha) * (Math.sqrt(value / ratio))) + minAlpha;
        } else {
            alpha = (float) ((maxAlpha - minAlpha) * (Math.sqrt((-value + 1) / (-ratio + 1)))) + minAlpha;
        }

        MathUtils.clamp(alpha, minAlpha, maxAlpha); //just in case

        return alpha;
    }

    /**
     *
     * @param value value from 0 to 1
     * @param time value for complete rotation
     * @param minAlpha value from 0 to 1, minimum alpha value
     * @param maxAlpha value from 0 to 1, maximum alpha value
     * @return value from 0 to 1, alpha multiplier
     */
    public static float getSinAlpha(float value, float time, float minAlpha, float maxAlpha) {
        float alpha = (float) (((maxAlpha - minAlpha) / 2f) * FastTrig.sin((Math.PI * 2f * value) / (time)) + ((maxAlpha - minAlpha) / 2f) + minAlpha);

        MathUtils.clamp(alpha, minAlpha, maxAlpha); //just in case

        return alpha;
    }
}
