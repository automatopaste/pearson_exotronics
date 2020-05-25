package data.missions.PSE_tester;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        // Set up the fleets
        api.initFleet(FleetSide.PLAYER, "BRS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "ISS", FleetGoal.ATTACK, true);

        // Set a small blurb for each fleet that shows up on the mission detail and
        // mission results screens to identify each side.
        api.setFleetTagline(FleetSide.PLAYER, "Your Fleet");
        api.setFleetTagline(FleetSide.ENEMY, "Opposing fleet");

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem("Dick around with these ships for science.");

        // Set up the player's fleet
        api.addToFleet(FleetSide.PLAYER, "PSE_serrano_Assault", FleetMemberType.SHIP, true);

        api.addToFleet(FleetSide.PLAYER, "PSE_denmark_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "PSE_denmark_Elite", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "PSE_denmark_CloseSupport", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "PSE_richmond_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "PSE_richmond_Overdriven", FleetMemberType.SHIP, false);

        api.addToFleet(FleetSide.PLAYER, "PSE_cassius_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "PSE_cassius_CloseSupport", FleetMemberType.SHIP, false);

        api.addToFleet(FleetSide.PLAYER, "PSE_kiruna_Assault", FleetMemberType.SHIP, false);



        //BOOTLEG XHAN STUFF
        api.addToFleet(FleetSide.PLAYER, "Boulo_HeavyDemo", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "Cheborog_Brawler", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "Karoba_Hauler", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "Meiche_Fueler", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "Occuklop_Decimator", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "OieHou_EliteGuard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "XHAN_Cheborog_Carrier_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "Olkzan_Eliteguard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "XHAN_Ketsil_CloseSupport", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "XHAN_Olkzan_Carrier_Defender", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "XHAN_Panrelka_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "XHAN_Pharrek_variant_EmperorsHammer", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "XHAN_Ubellop_Elite_Guard", FleetMemberType.SHIP, false);



        // Set up the enemy fleet
        api.addToFleet(FleetSide.ENEMY, "PSE_serrano_Assault", FleetMemberType.SHIP, false);

        api.addToFleet(FleetSide.ENEMY, "PSE_denmark_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "PSE_denmark_Elite", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "PSE_denmark_CloseSupport", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "PSE_richmond_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "PSE_richmond_Overdriven", FleetMemberType.SHIP, false);

        api.addToFleet(FleetSide.ENEMY, "PSE_cassius_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "PSE_cassius_CloseSupport", FleetMemberType.SHIP, false);

        api.addToFleet(FleetSide.ENEMY, "PSE_kiruna_Assault", FleetMemberType.SHIP, false);

        // Set up the map.
        float width = 10000f;
        float height = 10000f;
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
        api.addObjective(minX + width * 0.75f, minY + height * 0.25f, "comm_relay");
        api.addObjective(minX + width * 0.75f, minY + height * 0.75f, "nav_buoy");
        api.addObjective(minX + width * 0.25f, minY + height * 0.75f, "comm_relay");
        api.addObjective(minX + width * 0.5f, minY + height * 0.5f, "sensor_array");
    }
}
