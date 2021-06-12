package data.missions.PSE_tester;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import java.util.Collections;
import java.util.List;

public class MissionDefinition implements MissionDefinitionPlugin {

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        // Set up the fleets
        api.initFleet(FleetSide.PLAYER, "PCS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "ISS", FleetGoal.ATTACK, true);

        // Set a small blurb for each fleet that shows up on the mission detail and
        // mission results screens to identify each side.
        api.setFleetTagline(FleetSide.PLAYER, "Your Fleet");
        api.setFleetTagline(FleetSide.ENEMY, "Opposing fleet");

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem("Dick around with these ships for science.");

        // Set up the player's fleet
        //capitals
        /*boolean first = true;
        List<String> variants = Global.getSettings().getAllVariantIds();
        Collections.sort(variants);
        for (String variant : variants) {
            if (!variant.startsWith("PSE_") || variant.endsWith("_Hull") || variant.contains("drone")) {
                continue;
            }

            api.addToFleet(FleetSide.PLAYER, variant, FleetMemberType.SHIP, first);
            first = false;
        }*/

        api.addToFleet(FleetSide.PLAYER, "PSE_kingston_Assault", FleetMemberType.SHIP, true);
        //api.addToFleet(FleetSide.PLAYER, "PSE_kingston_Elite", FleetMemberType.SHIP, true);
        //api.addToFleet(FleetSide.PLAYER, "PSE_kingston_Strike", FleetMemberType.SHIP, true);
        //cruisers
        api.addToFleet(FleetSide.PLAYER, "PSE_leyland_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "PSE_serrano_Assault", FleetMemberType.SHIP, false);
        //api.addToFleet(FleetSide.PLAYER, "PSE_serrano_Overdriven", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "PSE_penrith_Assault", FleetMemberType.SHIP, false);
        //destroyers
        api.addToFleet(FleetSide.PLAYER, "PSE_denmark_Assault", FleetMemberType.SHIP, false);
        //api.addToFleet(FleetSide.PLAYER, "PSE_denmark_Elite", FleetMemberType.SHIP, false);
        //api.addToFleet(FleetSide.PLAYER, "PSE_denmark_CloseSupport", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "PSE_richmond_Assault", FleetMemberType.SHIP, false);
        //api.addToFleet(FleetSide.PLAYER, "PSE_richmond_Overdriven", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "PSE_armstrong_Assault", FleetMemberType.SHIP, false);
        //frigates
        api.addToFleet(FleetSide.PLAYER, "PSE_cassius_Assault", FleetMemberType.SHIP, false);
        //api.addToFleet(FleetSide.PLAYER, "PSE_cassius_CloseSupport", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "PSE_kiruna_Assault", FleetMemberType.SHIP, false);
        //civilian
        api.addToFleet(FleetSide.PLAYER, "PSE_eyre_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "PSE_torrens_Standard", FleetMemberType.SHIP, false);


        // Set up the enemy fleet
        api.addToFleet(FleetSide.ENEMY, "PSE_kingston_Elite", FleetMemberType.SHIP, false);

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
