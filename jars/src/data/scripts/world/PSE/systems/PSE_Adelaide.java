package data.scripts.world.PSE.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PSE_CampaignUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class PSE_Adelaide implements SectorGeneratorPlugin {
    @Override
    public void generate(SectorAPI sector) {
        //initialise system
        StarSystemAPI system = sector.createStarSystem("Adelaide");
        system.getLocation().set(-21000, 1000);
        system.setLightColor(new Color(199, 255, 209));
        system.setBackgroundTextureFilename("graphics/backgrounds/background2.jpg");

        //set up star
        PlanetAPI star = system.initStar("PSE_adelaide_star", "star_orange", 900, -21000, 1000, 250);
        //todo - set custom description

        //generate up to three entities in the centre of the system and returns the orbit radius of the furthest entity
        float innerOrbitDistance = StarSystemGenerator.addOrbitingEntities(system, star, StarAge.AVERAGE, 1, 3, 4000, 1, true);

        PlanetAPI newCaledonia = system.addPlanet("PSE_newCaledonia", star, "New Caledonia", "toxic", 180f, 140f, innerOrbitDistance + 6000, 365);
        newCaledonia.getSpec().setGlowColor(new Color(143, 255, 224, 255));
        newCaledonia.getSpec().setUseReverseLightForGlow(true);
        newCaledonia.setInteractionImage("illustrations", "urban03");
        newCaledonia.applySpecChanges();

        MarketAPI newCaledoniaMarketplace = PSE_CampaignUtils.addMarketplace(
                "pearson_exotronics",
                newCaledonia,
                null,
                "New Caledonia",
                6,
                new ArrayList<String>(Arrays.asList(
                        Conditions.LOW_GRAVITY,
                        Conditions.POPULATION_6,
                        Conditions.RARE_ORE_MODERATE,
                        Conditions.ORE_SPARSE,
                        Conditions.TOXIC_ATMOSPHERE,
                        Conditions.VOLATILES_ABUNDANT,
                        Conditions.RUINS_SCATTERED
                )),
                new ArrayList<String>(Arrays.asList(
                        Submarkets.SUBMARKET_OPEN,
                        Submarkets.SUBMARKET_STORAGE,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.GENERIC_MILITARY
                )),
                new ArrayList<String>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.SPACEPORT,
                        Industries.BATTLESTATION_MID,
                        Industries.MINING,
                        Industries.MILITARYBASE,
                        Industries.PLANETARYSHIELD,
                        Industries.ORBITALWORKS
                )),
                true,
                false
                );

        //nebula
        SectorEntityToken nebula = Misc.addNebulaFromPNG("data/campaign/terrain/eos_nebula.png", 0, 0, system, "terrain", "nebula_blue", 4, 4, StarAge.AVERAGE);

        //belts and stuff
        system.addAsteroidBelt(star, 80, innerOrbitDistance + 500, 255, 190, 220, Terrain.ASTEROID_BELT, null);
        system.addRingBand(star, "misc", "rings_asteroids0", 256f, 2, Color.white, 256f, innerOrbitDistance + 500, 200f, null, null);

        float radiusAfterSecondOrbitingEntities = StarSystemGenerator.addOrbitingEntities(system, star, StarAge.AVERAGE, 1, 3, innerOrbitDistance + 8000f, 2, false);

        //makeshift comm relay
        SectorEntityToken lexoniaMakeshiftRelay = system.addCustomEntity("PSE_adelaide_relay", "Adelaide No. 2 Relay", Entities.COMM_RELAY_MAKESHIFT, "pearson_exotronics");
        lexoniaMakeshiftRelay.setCircularOrbit(star, 270, innerOrbitDistance + 4000, 950);

        //jump points
        JumpPointAPI newCaledoniaJumpPoint = Global.getFactory().createJumpPoint("PSE_newCaledonia_jumpPoint", "New Caledonia Jump Point");
        OrbitAPI newCaledoniaJumpPointOrbit = Global.getFactory().createCircularOrbit(newCaledonia, 90f, 550f, 40f);
        newCaledoniaJumpPoint.setOrbit(newCaledoniaJumpPointOrbit);
        newCaledoniaJumpPoint.setRelatedPlanet(newCaledonia);
        newCaledoniaJumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(newCaledoniaJumpPoint);

        system.autogenerateHyperspaceJumpPoints(true, true);

        //cleanup
        //set up hyperspace editor plugin
        HyperspaceTerrainPlugin hyperspaceTerrainPlugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor nebulaEditor = new NebulaEditor(hyperspaceTerrainPlugin);

        //set up radius's in hyperspace of system
        float minHyperspaceRadius = hyperspaceTerrainPlugin.getTileSize() * 2f;
        float maxHyperspaceRadius = system.getMaxRadiusInHyperspace();

        //hyperstorm-b-gone (around system in hyperspace)
        nebulaEditor.clearArc(system.getLocation().x, system.getLocation().y, 0, minHyperspaceRadius + maxHyperspaceRadius, 0f, 360f, 0.25f);
    }
}
