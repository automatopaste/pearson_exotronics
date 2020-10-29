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
import java.util.List;

public class PSE_Adelaide implements SectorGeneratorPlugin {
    private List<MarketAPI> markets = new ArrayList<>();

    private static MarketAPI iucharMarket;

    @Override
    public void generate(SectorAPI sector) {
        //initialise system
        StarSystemAPI system = sector.createStarSystem("Adelaide");
        system.getLocation().set(1550, -15000);
        system.setLightColor(new Color(221, 255, 226));
        system.setBackgroundTextureFilename("graphics/backgrounds/background1.jpg");

        //set up star
        PlanetAPI star = system.initStar("PSE_adelaide_star", "star_orange", 900, 1550, -15000, 900);
        //todo.txt - set custom description

        //generate up to three entities in the centre of the system and returns the orbit radius of the furthest entity
        float innerOrbitDistance = StarSystemGenerator.addOrbitingEntities(system, star, StarAge.AVERAGE, 1, 3, 4000, 1, true);

        PlanetAPI newCaledonia = system.addPlanet("PSE_newCaledonia", star, "New Caledonia", "terran-eccentric", 30f, 140f, innerOrbitDistance + 1500, 365);
        newCaledonia.getSpec().setGlowTexture(Global.getSettings().getSpriteName("hab_glows", "volturn"));
        newCaledonia.getSpec().setGlowColor(new Color(0xFFFFFF));
        newCaledonia.getSpec().setUseReverseLightForGlow(true);
        newCaledonia.getSpec().setRotation(0f);
        newCaledonia.setInteractionImage("illustrations", "urban03");
        newCaledonia.setCustomDescriptionId("PSE_new_caledonia");
        newCaledonia.applySpecChanges();

        MarketAPI newCaledoniaMarketplace = PSE_CampaignUtils.addMarketplace(
                "pearson_exotronics",
                newCaledonia,
                null,
                "New Caledonia",
                7,
                new ArrayList<>(Arrays.asList(
                        Conditions.POPULATION_7,
                        Conditions.ORGANICS_COMMON,
                        Conditions.ORE_SPARSE,
                        Conditions.EXTREME_WEATHER,
                        Conditions.HOT,
                        Conditions.RUINS_SCATTERED,
                        Conditions.URBANIZED_POLITY,
                        Conditions.FARMLAND_RICH,
                        Conditions.HABITABLE,
                        Conditions.ROGUE_AI_CORE
                )),
                new ArrayList<>(Arrays.asList(
                        Submarkets.SUBMARKET_OPEN,
                        Submarkets.SUBMARKET_STORAGE,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.GENERIC_MILITARY
                )),
                new ArrayList<>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.SPACEPORT,
                        Industries.STARFORTRESS_MID,
                        Industries.MINING,
                        //Industries.PLANETARYSHIELD,
                        "PSE_malfunctioning_planetary_shield",
                        Industries.ORBITALWORKS,
                        Industries.HIGHCOMMAND,
                        Industries.WAYSTATION,
                        Industries.HEAVYBATTERIES,
                        Industries.FARMING
                )),
                true,
                false
        );
        markets.add(newCaledoniaMarketplace);

        //newCaledonia.getMarket().addSubmarket("custom_market");

        //new caledonia shades
        SectorEntityToken newCaledoniaShade1 = system.addCustomEntity("PSE_newCaledoniaShade1", "New Caledonian Solar Shade Alpha", "stellar_shade", "pearson_exotronics");
        SectorEntityToken newCaledoniaShade2 = system.addCustomEntity("PSE_newCaledoniaShade2", "New Caledonian Solar Shade Beta", "stellar_shade", "pearson_exotronics");
        newCaledoniaShade1.setCircularOrbitPointingDown(newCaledonia, 210f - 20f, 390, 365);
        newCaledoniaShade2.setCircularOrbitPointingDown(newCaledonia, 210f + 20f, 390, 365);
        newCaledoniaShade1.setCustomDescriptionId("stellar_shade");
        newCaledoniaShade2.setCustomDescriptionId("stellar_shade");

        //gas giant system
        PlanetAPI danu = system.addPlanet("PSE_danu", star, "Danu", "ice_giant", 150f, 400f, innerOrbitDistance + 7000f, 600);
        danu.getSpec().setPlanetColor(new Color(0xFFAB67));
        danu.getSpec().setCloudColor(new Color(156, 230, 250,150));
        danu.getSpec().setAtmosphereColor(new Color(181,180, 255,150));
        danu.applySpecChanges();
        //todo.txt - custom description

        system.addRingBand(danu, "misc", "rings_ice0", 256f, 3, Color.white, 256f, 1500, 30f, Terrain.RING, null);
        system.addRingBand(danu, "misc", "rings_dust0", 256f, 2, Color.white, 256f, 1850, 33f, Terrain.RING, null);

        PlanetAPI iuchar = system.addPlanet("PSE_iuchar", danu, "Iuchar", "barren-bombarded", 180f, 70f, 1000f, 100f);
        MarketAPI iucharMarketplace = PSE_CampaignUtils.addMarketplace(
                Factions.INDEPENDENT,
                iuchar,
                null,
                "Iuchar",
                4,
                new ArrayList<>(Arrays.asList(
                        Conditions.LOW_GRAVITY,
                        Conditions.POPULATION_4,
                        Conditions.RARE_ORE_ABUNDANT,
                        Conditions.NO_ATMOSPHERE,
                        Conditions.COLD
                )),
                new ArrayList<>(Arrays.asList(
                        Submarkets.SUBMARKET_OPEN,
                        Submarkets.SUBMARKET_STORAGE,
                        Submarkets.SUBMARKET_BLACK
                )),
                new ArrayList<>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.SPACEPORT,
                        Industries.MINING,
                        Industries.FUELPROD,
                        Industries.GROUNDDEFENSES
                )),
                false,
                false
        );
        markets.add(iucharMarketplace);
        iucharMarket = iucharMarketplace;

        PlanetAPI iucharba = system.addPlanet("PSE_iucharba", danu, "Iucharba", "cryovolcanic", 0f, 70f, 1000f, 100f);
        MarketAPI iucharbaMarketplace = PSE_CampaignUtils.addMarketplace(
                "pearson_exotronics",
                iucharba,
                null,
                "Iucharba",
                4,
                new ArrayList<>(Arrays.asList(
                        Conditions.LOW_GRAVITY,
                        Conditions.POPULATION_4,
                        Conditions.ORE_MODERATE,
                        Conditions.VOLATILES_DIFFUSE,
                        Conditions.THIN_ATMOSPHERE,
                        Conditions.COLD,
                        Conditions.RUINS_SCATTERED
                )),
                new ArrayList<>(Arrays.asList(
                        Submarkets.SUBMARKET_OPEN,
                        Submarkets.SUBMARKET_STORAGE,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.GENERIC_MILITARY
                )),
                new ArrayList<>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.SPACEPORT,
                        Industries.ORBITALSTATION_MID,
                        Industries.MINING,
                        Industries.MILITARYBASE,
                        Industries.HEAVYBATTERIES
                )),
                false,
                false
        );
        markets.add(iucharbaMarketplace);

        //nebula
        SectorEntityToken nebula = Misc.addNebulaFromPNG("data/campaign/terrain/eos_nebula.png", 0, 0, system, "terrain", "nebula_amber", 4, 4, StarAge.AVERAGE);

        //belts and stuff
        system.addAsteroidBelt(star, 80, innerOrbitDistance + 500, 255, 190, 220, Terrain.ASTEROID_BELT, null);
        system.addRingBand(star, "misc", "rings_asteroids0", 256f, 2, Color.white, 256f, innerOrbitDistance + 500, 200f, null, null);

        //makeshift comm relay
        SectorEntityToken makeshiftRelay = system.addCustomEntity("PSE_adelaide_relay", "Adelaide No. 2 Relay", Entities.COMM_RELAY_MAKESHIFT, "pearson_exotronics");
        makeshiftRelay.setCircularOrbit(star, 270, innerOrbitDistance + 4000, 950);

        //domain sensor array
        SectorEntityToken sensorArray = system.addCustomEntity("PSE_adelaide_sensor", "Adelaide No. 1 Scanning Array", Entities.SENSOR_ARRAY, "pearson_exotronics");
        sensorArray.setCircularOrbitPointingDown(danu, 90f, 2500f, 200f);

        //domain nav buoy
        SectorEntityToken navBuoy = system.addCustomEntity("PSE_adelaide_nav", "Adelaide No. 4 Nav Buoy", Entities.NAV_BUOY, "pearson_exotronics");
        navBuoy.setCircularOrbitPointingDown(star, 0f, innerOrbitDistance + 1000f, 200f);

        //stable points
        SectorEntityToken stable1 = system.addCustomEntity("PSE_adelaide_stable1", "Stable Location", Entities.STABLE_LOCATION, Factions.NEUTRAL);
        SectorEntityToken stable2 = system.addCustomEntity("PSE_adelaide_stable2", "Stable Location", Entities.STABLE_LOCATION, Factions.NEUTRAL);
        stable1.setCircularOrbitPointingDown(star, 120f, innerOrbitDistance + 10000f, 900f);
        stable2.setCircularOrbitPointingDown(star, 270f, innerOrbitDistance + 3000f, 300f);


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

    public List<MarketAPI> getMarkets() {
        return markets;
    }

    public static MarketAPI getIucharMarket() {
        return iucharMarket;
    }
}
