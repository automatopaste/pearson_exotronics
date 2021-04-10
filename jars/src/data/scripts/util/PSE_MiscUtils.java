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

    public static boolean isEntityInArc(CombatEntityAPI entity, Vector2f center, float centerAngle, float arcDeviation) {
        //Vector2f entityRelativeLocation = Vector2f.sub(entity.getLocation(), center, new Vector2f());
        //float entityAngle = VectorUtils.getFacing(entityRelativeLocation);
        //float rel = MathUtils.getShortestRotation(entityAngle, centerAngle);
        if (entity instanceof ShipAPI) {
            Vector2f point = getNearestPointOnShipBounds((ShipAPI) entity, center);
            return Misc.isInArc(centerAngle, arcDeviation * 2f, center, point);
        } else {
            return Misc.isInArc(centerAngle, arcDeviation * 2f, center, getNearestPointOnCollisionRadius(entity,  center));
        }
        //return rel < arcDeviation && rel > -arcDeviation;
    }

    public static Vector2f getVectorFromAToB(CombatEntityAPI a, CombatEntityAPI b) {
        return Vector2f.sub(b.getLocation(), a.getLocation(), new Vector2f());
    }

    public static Vector2f getRandomVectorInCircleRange(float maxRange, float minRange, Vector2f center) {
        float dist = (minRange + ((float) Math.random() * (maxRange - minRange)));
        Vector2f loc = new Vector2f(0f, dist);
        VectorUtils.rotate(loc, (float) Math.random() * 360f);
        Vector2f.add(loc, center, loc);
        return loc;
    }

    public static Vector2f getRandomVectorInCircleRangeWithDistanceMult(float maxRange, float minRange, Vector2f center, float mult) {
        float dist = (minRange + (mult * (maxRange - minRange)));
        Vector2f loc = new Vector2f(0f, dist);
        VectorUtils.rotate(loc, (float) Math.random() * 360f);
        Vector2f.add(loc, center, loc);
        return loc;
    }

    public static Vector2f getNearestPointOnCollisionRadius(CombatEntityAPI entity, Vector2f point) {
        return MathUtils.getPointOnCircumference(
                entity.getLocation(),
                entity.getCollisionRadius(),
                VectorUtils.getAngle(entity.getLocation(), point)
        );
    }

    public static Vector2f getNearestPointOnRadius(Vector2f center, float radius, Vector2f point) {
        return MathUtils.getPointOnCircumference(
                center,
                radius,
                VectorUtils.getAngle(center, point)
        );
    }

    public static Vector2f getNearestPointOnShipBounds(ShipAPI ship, Vector2f point) {
        BoundsAPI bounds = ship.getExactBounds();
        if (bounds == null) {
            return getNearestPointOnCollisionRadius(ship, point);
        } else {
            Vector2f closest = ship.getLocation();
            float distSquared = 0f;
            for (BoundsAPI.SegmentAPI segment : bounds.getSegments()) {
                Vector2f tmpcp = MathUtils.getNearestPointOnLine(point, segment.getP1(), segment.getP2());
                float distSquaredTemp = MathUtils.getDistanceSquared(tmpcp, point);
                if (distSquaredTemp < distSquared) {
                    distSquared = distSquaredTemp;
                    closest = tmpcp;
                }
            }
            return closest;
        }
    }

    private void rotateDecoThruster(WeaponAPI weapon, float angle, float thrust, ShipAPI ship, float amount, float woobleVariation){
        float targetAngle = angle + ship.getFacing();
        float woobleOffset = (float) FastTrig.sin((3.14159f / 2f) * ship.getFullTimeDeployed() * thrust) * woobleVariation;
        targetAngle += woobleOffset;

        float delta = MathUtils.getShortestRotation(weapon.getCurrAngle(), targetAngle);
        delta *= amount;

        float maxRotationSpeed = 5f * amount; //5 degrees/second, but limited to degrees per frame
        MathUtils.clamp(delta, -maxRotationSpeed, maxRotationSpeed);

        weapon.setCurrAngle(weapon.getCurrAngle() + delta);
    }
}
