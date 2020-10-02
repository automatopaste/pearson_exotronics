package data.missions.PSE_KnightsOfCydonia;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MissionDefinition implements MissionDefinitionPlugin {
    private static final List<String> enemyVariants = new ArrayList<String>();
    static {
        enemyVariants.add("paragon_Raider");

        enemyVariants.add("aurora_Assault_Support");
        enemyVariants.add("aurora_Balanced");
        enemyVariants.add("doom_Support");
        enemyVariants.add("heron_Strike");
        enemyVariants.add("heron_Strike");

        enemyVariants.add("harbinger_Strike");
        enemyVariants.add("medusa_Attack");
        enemyVariants.add("medusa_Attack");
        //enemyVariants.add("medusa_Attack");
        enemyVariants.add("drover_Starting");
        enemyVariants.add("drover_Starting");

        enemyVariants.add("scarab_Experimental");
        enemyVariants.add("scarab_Experimental");
        //enemyVariants.add("afflictor_Strike");
        enemyVariants.add("tempest_Attack");
        enemyVariants.add("tempest_Attack");
        //enemyVariants.add("tempest_Attack");
        enemyVariants.add("omen_PD");
        //enemyVariants.add("omen_PD");
        enemyVariants.add("brawler_tritachyon_Standard");
    }

    private static final String playerFlagship = "PSE_kingston_sod_Special";
    private static final String playerFlagshipName = "PCS Tasmania";

    private static final List<String> playerVariants = new ArrayList<String>();
    static {
        playerVariants.add("PSE_leyland_sod_Special");
        playerVariants.add("PSE_serrano_sod_Special");
        playerVariants.add("PSE_serrano_sod_Special");
        playerVariants.add("heron_Attack");

        playerVariants.add("PSE_denmark_sod_Special");
        //playerVariants.add("PSE_denmark_sod_Special");
        playerVariants.add("PSE_richmond_sod_Special");
        //playerVariants.add("PSE_richmond_sod_Special");

        playerVariants.add("PSE_cassius_Assault");
        playerVariants.add("PSE_cassius_sod_Special");
        playerVariants.add("PSE_cassius_CloseSupport");
        playerVariants.add("PSE_kiruna_sod_Special");
        playerVariants.add("PSE_kiruna_Assault");
        playerVariants.add("PSE_eyre_Elite");
        //playerVariants.add("PSE_eyre_sod_Special");
    }

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        api.addPlanet(0f, 0f, 150f, "barren-bombarded", 0f, true);

        api.addBriefingItem("The PCS Tasmania must survive");
        api.addBriefingItem("The enemy flagship will defeat the Tasmania in a fair fight");
        api.defeatOnShipLoss(playerFlagshipName);
        api.addBriefingItem("Destroy those conniving bastards");

        api.initFleet(FleetSide.PLAYER, "PCS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "TTS", FleetGoal.ATTACK, true);

        api.setFleetTagline(FleetSide.PLAYER, "Cydonian Defence Fleet");
        api.setFleetTagline(FleetSide.ENEMY, "Tri-Tachyon Planet-Killer Escort Force");

        api.setBackgroundGlowColor(new Color(255, 156, 20, 59));

        boolean f = true;
        for (String variant : enemyVariants) {
            api.addToFleet(FleetSide.ENEMY, variant, FleetMemberType.SHIP, f);
            f = false;
        }

        api.addToFleet(FleetSide.PLAYER, playerFlagship, FleetMemberType.SHIP, playerFlagshipName, true);

        for (String variant : playerVariants) {
            api.addToFleet(FleetSide.PLAYER, variant, FleetMemberType.SHIP, false);
        }

        // Set up the map.
        float width = 15000f;
        float height = 15000f;
        api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);

        float minX = -width / 2;
        float minY = -height / 2;

        for (int i = 0; i < 50; i++) {
            float x = (float) Math.random() * width - width / 2;
            float y = (float) Math.random() * height - height / 2;
            float radius = 100f + (float) Math.random() * 400f;
            api.addNebula(x, y, radius);
        }

        // Add objectives
        api.addObjective(minX + width * 0.25f, minY + height * 0.25f, "nav_buoy");
        api.addObjective(minX + width * 0.35f, minY + height * 0.65f, "comm_relay");
        api.addObjective(minX + width * 0.75f, minY + height * 0.45f, "nav_buoy");
        api.addObjective(minX + width * 0.65f, minY + height * 0.35f, "comm_relay");
        api.addObjective(minX + width * 0.5f, minY + height * 0.25f, "sensor_array");
    }
}
