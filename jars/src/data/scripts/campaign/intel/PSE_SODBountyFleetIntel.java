package data.scripts.campaign.intel;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BreadcrumbSpecial;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PSE_SODBountyFleetIntel extends BaseIntelPlugin implements EveryFrameScript, FleetEventListener {
    private static Logger log = Global.getLogger(PSE_SODBountyFleetIntel.class);

    public static final String SPECIAL_BOUNTY_KEY = "$PSE_specialBountyDefeated";

    public enum BountyState {
        ACTIVE,
        DEFEATED_PLAYER,
        DEFEATED_OTHER
    }
    private CampaignFleetAPI fleet;
    private FactionAPI bountyFaction;
    private SectorEntityToken hideoutLocation;
    private BountyState state = BountyState.ACTIVE;
    private PersonAPI person;
    private int payment;
    private PersonAPI bountyGiver;

    public PSE_SODBountyFleetIntel(int payment, PersonAPI bountyGiver) { //todo.txt - add detailed intel descriptions
        this.payment = payment;
        this.bountyGiver = bountyGiver;

        pickHideoutLocation();

        bountyFaction = pickBountyFaction();

        initPerson();

        spawnFleet();

        log.info(String.format("Starting SOD stolen ships bounty for person %s from faction [%s] ", person.getName().getFullName(), bountyFaction.getDisplayName()));
    }

    private void pickHideoutLocation() {
        WeightedRandomPicker<StarSystemAPI> systemPicker = new WeightedRandomPicker<>();
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            float mult = 0f;

            if (system.hasPulsar()) continue;

            if (system.hasTag(Tags.THEME_MISC_SKIP)) {
                mult = 1f;
            } else if (system.hasTag(Tags.THEME_MISC)) {
                mult = 3f;
            } else if (system.hasTag(Tags.THEME_REMNANT_NO_FLEETS)) {
                mult = 3f;
            } else if (system.hasTag(Tags.THEME_RUINS)) {
                mult = 5f;
            } else if (system.hasTag(Tags.THEME_REMNANT_DESTROYED)) {
                mult = 3f;
            } else if (system.hasTag(Tags.THEME_CORE_UNPOPULATED)) {
                mult = 1f;
            }

            for (MarketAPI ignored : Misc.getMarketsInLocation(system)) {
                //if (market.isHidden()) continue;
                mult = 0f;
                break;
            }

            float distToPlayer = Misc.getDistanceToPlayerLY(system.getLocation());
            float noSpawnRange = Global.getSettings().getFloat("personBountyNoSpawnRangeAroundPlayerLY");
            if (distToPlayer < noSpawnRange) mult = 0f;

            if (mult <= 0) continue;

            float weight = system.getPlanets().size();
            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.isStar()) continue;
                if (planet.getMarket() != null) {
                    float h = planet.getMarket().getHazardValue();
                    if (h <= 0f) weight += 5f;
                    else if (h <= 0.25f) weight += 3f;
                    else if (h <= 0.5f) weight += 1f;
                }
            }

            float dist = system.getLocation().length();
            float distMult = Math.max(0, 50000f - dist);

            systemPicker.add(system, weight * mult * distMult);
        }

        StarSystemAPI system = systemPicker.pick();

        if (system != null) {
            WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<>();
            for (SectorEntityToken planet : system.getPlanets()) {
                if (planet.isStar()) continue;
                if (planet.getMarket() != null &&
                        !planet.getMarket().isPlanetConditionMarketOnly()) continue;

                picker.add(planet);
            }
            hideoutLocation = picker.pick();
        }

        if (hideoutLocation == null) {
            log.info("Hideout location was null");
            endImmediately();
        }
    }

    @Override
    protected void advanceImpl(float amount) {
        //float days = Global.getSector().getClock().convertToDays(amount);

        /*if (!isDone()) { //used for timeout
            boolean canEnd = fleet == null || !fleet.isInCurrentLocation();
            if (canEnd) {
                log.info(String.format("Ending bounty on %s from %s", person.getName().getFullName(), faction.getDisplayName()));
                sendUpdateIfPlayerHasIntel(null, true);

                Global.getSector().getMemoryWithoutUpdate().set(SPECIAL_BOUNTY_KEY, true);
                log.info("Bounty completed, setting memory data");

                cleanUpFleetAndEndIfNecessary();
                return;
            }
        }*/

        if (fleet == null) return;

        if (fleet.getFlagship() == null || fleet.getFlagship().getCaptain() != person) {
            boolean current = fleet.isInCurrentLocation();
            sendUpdateIfPlayerHasIntel(null, !current);

            state = BountyState.DEFEATED_OTHER;
            setMemory(state);

            cleanUpFleetAndEndIfNecessary();
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        float opad = 10f;

        info.addImage(person.getPortraitSprite(), width, 128, opad);

        String type = "an agent from " + bountyFaction.getDisplayNameWithArticle();
        LabelAPI desc = info.addPara(
                bountyGiver.getName().getFullName() + " has hired you to bring " +
                        person.getName().getFullName() + ", " + type + ", to justice. A " +
                payment + " credit reward has been offered, along with any salvage gained from the battle.",
                opad
        );

        desc.setHighlight(bountyGiver.getName().getFullName(), person.getName().getFullName(), bountyFaction.getDisplayNameWithArticle(), payment + "");
        desc.setHighlightColors(bountyGiver.getFaction().getBaseUIColor(), person.getFaction().getBaseUIColor(), bountyFaction.getBaseUIColor(), h);

        if (state == BountyState.ACTIVE) {
            if (hideoutLocation != null) {
                SectorEntityToken fake = hideoutLocation.getContainingLocation().createToken(0, 0);
                fake.setOrbit(Global.getFactory().createCircularOrbit(hideoutLocation, 0, 1000, 100));

                String loc = BreadcrumbSpecial.getLocatedString(fake);
                loc = loc.replaceAll("orbiting", "hiding out near");
                loc = loc.replaceAll("located in", "hiding out in");
                String sheIs = "She is";
                if (person.getGender() == FullName.Gender.MALE) sheIs = "He is";
                info.addPara(sheIs + " rumored to be " + loc + ".", opad);
            }

            int cols = 7;
            float iconSize = width / cols;

            List<FleetMemberAPI> list = new ArrayList<>();
            Random random = new Random(person.getNameString().hashCode() * 170000);

            List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
            int max = 7;
            for (FleetMemberAPI member : members) {
                if (list.size() >= max) break;

                if (member.isFighterWing()) continue;

                if (member.getHullId().endsWith("_sod")) continue; //check for sod nature

                float prob = (float) member.getFleetPointCost() / 20f;
                prob += (float) max / (float) members.size();
                if (member.isFlagship()) prob = 1f;

                if (random.nextFloat() > prob) continue;

                FleetMemberAPI copy = Global.getFactory().createFleetMember(FleetMemberType.SHIP, member.getVariant());
                if (member.isFlagship()) {
                    copy.setCaptain(person);
                }
                list.add(copy);
            }

            if (!list.isEmpty()) {
                String her = "her";
                if (person.getGender() == FullName.Gender.MALE) her = "his";
                info.addPara("The bounty posting also contains partial intel on some of the ships under " + her + " command.", opad);
                info.addShipList(cols, 1, iconSize, getFactionForUIColors().getBaseUIColor(), list, opad);

                int num = members.size() - list.size();
                num = Math.round((float)num * (1f + random.nextFloat() * 0.5f));

                if (num < 5) num = 0;
                else if (num < 10) num = 5;
                else if (num < 20) num = 10;
                else num = 20;

                if (num > 1) {
                    info.addPara("The intel assessment notes the fleet may contain upwards of %s other ships" +
                            " of lesser significance.", opad, h, "" + num);
                } else {
                    info.addPara("The intel assessment notes the fleet may contain several other ships" +
                            " of lesser significance.", opad);
                }
            }

            Color division = Global.getSector().getFaction("pearson_division").getBaseUIColor();
            List<FleetMemberAPI> specialMembers = new ArrayList<>();
            int maxSpecial = 5;
            for (FleetMemberAPI member : members) {
                if (member.isFighterWing()) continue;
                if (specialMembers.size() >= maxSpecial) break;

                if (member.getHullId().endsWith("_sod")) specialMembers.add(member);
            }
            if (!specialMembers.isEmpty()) {
                info.addPara(
                        bountyGiver.getName().getLast() +
                        "'s intel indicates an inventory of stolen Special Operations Division warships.",
                       opad,
                        division,
                        "Special Operations Division",
                        bountyGiver.getName().getLast()
                );
                info.addShipList(cols, 1, iconSize, division, specialMembers, opad);
            }
        }
    }

    @Override
    public String getIcon() {
        return person.getPortraitSprite();
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
         info.addPara(getName(), getTitleColor(mode),0f);

         addBulletPoints(info, mode);
    }

    private void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode) {
        Color h = Misc.getHighlightColor();
        float pad = 3f;
        float opad = 10f;

        float initPad = pad;
        if (mode == ListInfoMode.IN_DESC) initPad = opad;

        Color tc = getBulletColorForMode(mode);

        bullet(info);

        switch (state) {
            case ACTIVE:
                if (mode == ListInfoMode.IN_DESC) {
                    info.addPara(
                            "Special Operations Division intelligence indicates they are hiding in the " +
                                    hideoutLocation.getStarSystem().getNameWithTypeIfNebula() + " system, near " + hideoutLocation.getFullName() + ".",
                            initPad
                    );

                    /*int days = (int) (duration - elapsedDays);
                    if (days <= 1) {
                        days = 1;
                    }
                    addDays(info, "remaining", days, tc);*/
                } else {
                    info.addPara("Faction: " + bountyFaction.getDisplayName(), initPad, tc, bountyFaction.getBaseUIColor(), bountyFaction.getDisplayName());

                    /*if (!isEnding()) {
                        int days = (int) (duration - elapsedDays);
                        String daysStr = "days";
                        if (days <= 1) {
                            days = 1;
                            daysStr = "day";
                        }
                        info.addPara("%s reward, %s " + daysStr + " remaining", 0f, tc,
                                h, Misc.getDGSCredits(bountyCredits), "" + days);
                    }*/
                }
                unindent(info);
                break;
            case DEFEATED_OTHER:
                info.addPara("Bounty defeated by other fleet.", initPad);
                break;
            case DEFEATED_PLAYER:
                info.addPara("Return to " + bountyGiver.getNameString() + " to receive the %s bounty completion payment.", initPad, tc, h, Misc.getDGSCredits(payment));
                break;
        }

        unindent(info);
    }

    private void initPerson() {
        int personLevel = 5;
        personLevel += (int) ((Global.getSector().getPlayerStats().getLevel() / Global.getSettings().getLevelupPlugin().getMaxLevel()) * 20f);
        person = OfficerManagerEvent.createOfficer(bountyFaction, personLevel, true, false);
    }

    public String getName() {
        //String n = person.getName().getFullName();
        String sting = "Stolen SOD Warships - ";

        switch (state) {
            case ACTIVE:
                return sting + "Special Bounty";
            case DEFEATED_PLAYER:
                return sting + "Completed";
            case DEFEATED_OTHER:
                return sting + "Ended";
        }
        return sting + "Special Bounty";
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return bountyFaction;
    }

    @Override
    public String getSmallDescriptionTitle() {
        return getName();
    }

    private void cleanUpFleetAndEndIfNecessary() {
        if (state == BountyState.ACTIVE) {
            state = BountyState.DEFEATED_OTHER;
        }
        setMemory(state);

        log.info("Cleaning up bounty fleet");
        if (fleet != null) {
            Misc.makeUnimportant(fleet, "PSE_SOD_special_bounty");
            fleet.clearAssignments();
            if (hideoutLocation != null) {
                fleet.getAI().addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, hideoutLocation, 1000000f, null);
            } else {
                fleet.despawn();
            }
            fleet = null; //can't null it because description uses it
        }
        if (!isEnding() && !isEnded()) {
            endAfterDelay();
        }
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_BOUNTY);
        tags.add(bountyFaction.getId());
        tags.add(Tags.INTEL_IMPORTANT);
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        Constellation c = hideoutLocation.getConstellation();
        SectorEntityToken entity = null;
        if (c != null && map != null) {
            entity = map.getConstellationLabelEntity(c);
        }
        if (entity == null) entity = hideoutLocation;
        return entity;
    }

    private FactionAPI pickBountyFaction() {
        Random r = new Random();
        List<FactionAPI> suitable = new ArrayList<>();
        FactionAPI pearson = Global.getSector().getFaction("pearson_exotronics");
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (pearson.getRelationship(faction.getId()) < 0.1f) {
                suitable.add(faction);
            }
        }
        suitable.remove(Global.getSector().getFaction(Factions.PIRATES));
        suitable.remove(Global.getSector().getFaction(Factions.LUDDIC_PATH));
        List<FactionAPI> temp = new ArrayList<>();
        for (FactionAPI faction : suitable) {
            if (!faction.isShowInIntelTab()) {
                temp.add(faction);
            }
        }
        for (FactionAPI faction : temp) {
            suitable.remove(faction);
        }
        if (!suitable.contains(Global.getSector().getFaction(Factions.TRITACHYON))) {
            suitable.add(Global.getSector().getFaction(Factions.TRITACHYON));
        }
        return suitable.get(r.nextInt(suitable.size()));
    }

    private void spawnFleet() {
        float min = 30f;
        float inf = 250f;
        float frac = (float) Global.getSector().getPlayerStats().getLevel() / 50f;

        float points = 0f;
        for (int i = 0; i < 10; i++) {
            if (Math.random() < 0.5f) {
                points += 5f + (15f * frac);
            }
        }
        points += min;
        points += inf * frac;

        FleetParamsV3 params = new FleetParamsV3(
                null,
                hideoutLocation.getLocationInHyperspace(),
                bountyFaction.getId(),
                0.75f,
                FleetTypes.PERSON_BOUNTY_FLEET,
                points,
                0f,
                0f,
                0f,
                0f,
                0f,
                0.75f
        );
        params.ignoreMarketFleetSizeMult = true;

        CampaignFleetAPI newFleet = FleetFactoryV3.createFleet(params);
        if (newFleet == null || newFleet.isEmpty()) {
            log.info("Fleet is null or empty");
            fleet = null;
            return;
        }

        Misc.makeImportant(newFleet, "PSE_SOD_special_bounty");

        injectSODShipMembers(newFleet);

        newFleet.getFleetData().sort();

        newFleet.setNoFactionInName(false);
        newFleet.setName("Agent Fleet");

        newFleet.setCommander(person);
        newFleet.getFlagship().setCaptain(person);
        FleetFactoryV3.addCommanderSkills(person, newFleet,null);

        newFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PIRATE, true);
        newFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_NO_MILITARY_RESPONSE, true);

        LocationAPI location = hideoutLocation.getContainingLocation();
        location.addEntity(newFleet);
        newFleet.setLocation(hideoutLocation.getLocation().x - 500f, hideoutLocation.getLocation().y + 500f);
        newFleet.getAI().addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, hideoutLocation, 1000000f, null);

        newFleet.getFleetData().ensureHasFlagship();

        fleet = newFleet;
    }

    private void injectSODShipMembers(CampaignFleetAPI fleet) {
        int fp = fleet.getFleetPoints();
        int SODfp = (int) (fp / 3f);
        log.info("Creating SOD fleet with " + SODfp + " points from fleet with " + fp + " points");
        FleetParamsV3 SODFleetParams = new FleetParamsV3(
                new Vector2f(),
                "pearson_division",
                1f,
                FleetTypes.PATROL_MEDIUM,
                SODfp,
                0f,
                0f,
                0f,
                0f,
                0f,
                0.7f
                );

        CampaignFleetAPI SODFleet = FleetFactoryV3.createFleet(SODFleetParams);
        int num = (int) (fleet.getFleetSizeCount() / 6f);
        if (num < 2) num = 2;

        for (FleetMemberAPI member : SODFleet.getMembersWithFightersCopy()) {
            if (member.isFighterWing()) continue;
            if (num > 0) {
                num--;
                fleet.getFleetData().addFleetMember(member);
            } else {
                break;
            }
        }
    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
        if (isDone() || state != BountyState.ACTIVE) return;

        if (this.fleet == fleet) {
            fleet.setCommander(fleet.getFaction().createRandomPerson());
            sendUpdateIfPlayerHasIntel(null, true);
            cleanUpFleetAndEndIfNecessary();
        }
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (isDone()) return;

        //log.info("Battle reported between " + fleet.getNameWithFactionKeepCase() + " and " + battle.getClosestInvolvedFleetTo(fleet).getNameWithFactionKeepCase());

        if (battle.isInvolved(fleet) && !battle.isPlayerInvolved()) {
            if (fleet.getFlagship() == null || fleet.getFlagship().getCaptain() != person) {
                fleet.setCommander(fleet.getFaction().createRandomPerson());
                state = BountyState.DEFEATED_OTHER;
                sendUpdateIfPlayerHasIntel(null, true);

                setMemory(state);

                cleanUpFleetAndEndIfNecessary();
                return;
            }
        }

        if (!battle.isPlayerInvolved() || !battle.isInvolved(fleet) || battle.onPlayerSide(fleet)) {
            return;
        }

        // didn't destroy the original flagship
        if (fleet.getFlagship() != null && fleet.getFlagship().getCaptain() == person) return;

        state = BountyState.DEFEATED_PLAYER;
        sendUpdateIfPlayerHasIntel(null, false);

        setMemory(state);

        cleanUpFleetAndEndIfNecessary();
    }

    private void setMemory(BountyState state) {
        Global.getSector().getMemoryWithoutUpdate().set(SPECIAL_BOUNTY_KEY, state);
        log.info("Bounty completed, setting memory data with state " + state.toString());
    }

    public CampaignFleetAPI getFleet() {
        return fleet;
    }

    public PersonAPI getPerson() {
        return person;
    }

    public FactionAPI getBountyFaction() {
        return bountyFaction;
    }

    public SectorEntityToken getHideoutLocation() {
        return hideoutLocation;
    }
}
