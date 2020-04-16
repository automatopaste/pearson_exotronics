package data.scripts.util;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;

import static org.lazywizard.lazylib.combat.AIUtils.getEnemiesOnMap;

public final class PSE_DroneUtil {

    private PSE_DroneUtil() {
    }

    //directly modified from lazywizard's lazylib getNearestEnemy
    public static ShipAPI getNearestEnemyNonFighterShip(final CombatEntityAPI entity) {
        ShipAPI closest = null;
        float closestDistance = Float.MAX_VALUE;
        for (ShipAPI tmp : getEnemiesOnMap(entity)) {
            if (tmp.isFighter()) {
                continue;
            }
            float distance = MathUtils.getDistance(tmp, entity.getLocation());
            if (distance < closestDistance) {
                closest = tmp;
                closestDistance = distance;
            }
        }
        return closest;
    }

    //directly modified from lazywizard's lazylib getNearestEnemy
    public static ShipAPI getNearestEnemyFighter(final CombatEntityAPI entity) {
        ShipAPI closest = null;
        float closestDistance = Float.MAX_VALUE;
        for (ShipAPI tmp : getEnemiesOnMap(entity)) {
            if (!tmp.isFighter()) {
                continue;
            }
            float distance = MathUtils.getDistance(tmp, entity.getLocation());
            if (distance < closestDistance) {
                closest = tmp;
                closestDistance = distance;
            }
        }
        return closest;
    }
}
