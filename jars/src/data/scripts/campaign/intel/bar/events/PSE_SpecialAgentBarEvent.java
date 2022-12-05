package data.scripts.campaign.intel.bar.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.PSE_SODCamp;
import data.scripts.campaign.PSE_SODCampEventListener;
import data.scripts.campaign.intel.PSE_SODBountyFleetIntel;
import data.scripts.campaign.intel.PSE_SODCampDeliveryIntel;
import org.apache.log4j.Logger;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PSE_SpecialAgentBarEvent extends BaseBarEventWithPerson {
    private static final String AGENT_PORTRAIT = "graphics/PSE/portraits/PSE_RobertPool.png";
    private static final String FIRST_NAME = "Robert";
    private static final String LAST_NAME = "Pool";
    private static final FullName.Gender GENDER = FullName.Gender.MALE;

    private static Logger log = Global.getLogger(PSE_SpecialAgentBarEvent.class);

    private static final List<String> prompts = new ArrayList<>(Arrays.asList(
            "$NAME is watching the bar occupants with one eye while manipulating a virtual keyboard.",
            "$NAME is in the lounge, and appears to be absorbed in fiddling with some circuitry exposed from an unfamiliar gadget."
    ));

    /*
    STAGES:
    First meeting: require commission, if player interested
    Second meeting: go fight dangerous fleet, from hostile faction with x1.5 FP to player, injected with "stolen" SOD variants
    Third+ meeting: deliver parcel of illegal/powerful goods from whitelist to support covert SOD camp, create intel item. give location of camp and quantity of goods, generates agent at market, requires transponder off. can purchase from market afterwards
    */

    private enum OptionDialog {
        INITIAL,
        INTRO_TO_FLEET_QUEST,
        GIVE_FLEET_QUEST,
        FLEET_QUEST_ACCEPTED,
        FLEET_DEFEATED,
        DELIVER_PARCEL_TO_HIDDEN_CAMP,
        ACCEPT_DELIVERY,
        DELIVERY_SUCCESSFUL,
        LEAVE,
        LEAVE_DECLINE_FLEET_QUEST,
        LEAVE_DECLINE_DELIVERY
    }

    private enum AgentEventStages {
        INTRODUCTION,
        GIVE_FLEET_BOUNTY,
        FLEET_BOUNTY_ACTIVE,
        FLEET_BOUNTY_COMPLETED,
        FLEET_BOUNTY_DEFEATED_BY_OTHER,
        GIVE_SUBMARKET_QUEST,
        SUBMARKET_QUEST_ACTIVE,
        SUBMARKET_QUEST_COMPLETED,
        IDLE
    }
    private static final List<AgentEventStages> STARTING_STAGES = new ArrayList<>();
    static {
        STARTING_STAGES.add(AgentEventStages.GIVE_FLEET_BOUNTY);
        STARTING_STAGES.add(AgentEventStages.GIVE_SUBMARKET_QUEST);
    }
    private AgentEventStages stage = AgentEventStages.INTRODUCTION;

    private static final float FLEET_BOUNTY_REWARD_BASE = 150000f;
    private float bountyReward = 0f;
    private static boolean hasDefeatedInitialFleet = false;
    private static boolean hasFoundInitialSubmarket = false;
    private static final String QUEST_TIME_TRACKER_KEY = "$PSE_questTimeTracker";
    private static final float QUEST_INTERVAL_DAYS = 10f;
    private PSE_SODBountyFleetIntel fleetIntel;
    private PSE_SODCampDeliveryIntel campIntel;
    private PSE_SODCamp camp;

    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);

        dialog.getVisualPanel().showPersonInfo(person, true);
        done = false;

        log.info("Initialising SOD Agent bar event");

        if (Global.getSector().getMemoryWithoutUpdate().get(PSE_SODBountyFleetIntel.SPECIAL_BOUNTY_KEY) instanceof PSE_SODBountyFleetIntel.BountyState) {
            PSE_SODBountyFleetIntel.BountyState bountyState = (PSE_SODBountyFleetIntel.BountyState) Global.getSector().getMemoryWithoutUpdate().get(PSE_SODBountyFleetIntel.SPECIAL_BOUNTY_KEY);
            log.info("Detecting if special bounty is completed: " + bountyState.toString());
            if (stage == AgentEventStages.FLEET_BOUNTY_ACTIVE) {
                if (bountyState == PSE_SODBountyFleetIntel.BountyState.DEFEATED_PLAYER) {
                    stage = AgentEventStages.FLEET_BOUNTY_COMPLETED;
                } else if (bountyState == PSE_SODBountyFleetIntel.BountyState.DEFEATED_OTHER) {
                    stage = AgentEventStages.FLEET_BOUNTY_DEFEATED_BY_OTHER;
                }
            }
        }

        boolean found = false;
        if (Global.getSector().getMemoryWithoutUpdate().get(PSE_SODCampDeliveryIntel.DELIVERED_CAMP_KEY) instanceof PSE_SODCamp) {
            PSE_SODCamp delivered = (PSE_SODCamp) Global.getSector().getMemoryWithoutUpdate().get(PSE_SODCampDeliveryIntel.DELIVERED_CAMP_KEY);

            if (delivered.equals(camp) && stage == AgentEventStages.SUBMARKET_QUEST_ACTIVE) {
                stage = AgentEventStages.SUBMARKET_QUEST_COMPLETED;
                Global.getSector().getMemoryWithoutUpdate().set(PSE_SODCampDeliveryIntel.DELIVERED_CAMP_KEY, null);
                found = true;
            }
        }
        log.info("Detecting if SOD camp has been delivered to: " + found);

        if (stage == AgentEventStages.IDLE) {
            long previousTimestamp = Global.getSector().getMemoryWithoutUpdate().getLong(QUEST_TIME_TRACKER_KEY);
            float elapsed = Global.getSector().getClock().getElapsedDaysSince(previousTimestamp);
            long curr = Global.getSector().getClock().getTimestamp();

            if (hasDefeatedInitialFleet) {
                if (hasFoundInitialSubmarket) {
                    if (elapsed >= QUEST_INTERVAL_DAYS) {
                        stage = pickRandomEventStart();

                        Global.getSector().getMemoryWithoutUpdate().set(QUEST_TIME_TRACKER_KEY, curr);
                    }
                } else {
                    if (elapsed >= QUEST_INTERVAL_DAYS) {
                        stage = AgentEventStages.GIVE_SUBMARKET_QUEST;

                        Global.getSector().getMemoryWithoutUpdate().set(QUEST_TIME_TRACKER_KEY, curr);
                    }
                }
            } else {
                if (elapsed >= QUEST_INTERVAL_DAYS) {
                    stage = AgentEventStages.GIVE_FLEET_BOUNTY;

                    Global.getSector().getMemoryWithoutUpdate().set(QUEST_TIME_TRACKER_KEY, curr);
                }
            }
        }

        optionSelected(null, OptionDialog.INITIAL);
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.addPromptAndOption(dialog, memoryMap);

        regen(dialog.getInteractionTarget().getMarket());

        TextPanelAPI text = dialog.getTextPanel();

        Color promptColour;
        if (stage == AgentEventStages.INTRODUCTION) {
            promptColour = Misc.getStoryOptionColor();

            text.addPara("At the back of the bar sits a scarred " + getManOrWoman() + " watching you with their one good eye");
            dialog.getOptionPanel().addOption("Approach the scarred spacer.", this, promptColour, null);
        } else {
            text.addPara(getRandomPromptLine());
            dialog.getOptionPanel().addOption("Approach " + person.getName().getLast(), this);
        }
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        OptionPanelAPI options = dialog.getOptionPanel();
        TextPanelAPI text = dialog.getTextPanel();
        options.clearOptions();

        OptionDialog option = (OptionDialog) optionData;

        switch (option) {
            case INITIAL:
                switch (stage) {
                    case INTRODUCTION:
                        text.addPara(
                                "You pull up a stool and sit down on the illuminated side of the table."
                        );

                        options.addOption("Ask them why they're staring at you.", OptionDialog.INTRO_TO_FLEET_QUEST);
                        options.addOption("Snort at them and leave.", OptionDialog.LEAVE);
                        break;
                    case GIVE_FLEET_BOUNTY:
                        defaultGreet(text);

                        options.addOption("Ask if there's any work available.", OptionDialog.GIVE_FLEET_QUEST);
                        break;
                    case FLEET_BOUNTY_ACTIVE:
                        defaultGreet(text);

                        text.addPara("\"Haven't dealt with that agent yet? My neck's on the line here...\"");

                        options.addOption("\"Still working out the details.\"", OptionDialog.LEAVE);
                        break;
                    case FLEET_BOUNTY_COMPLETED:
                        defaultGreet(text);

                        options.addOption("Explain your success.", OptionDialog.FLEET_DEFEATED);
                        break;
                    case FLEET_BOUNTY_DEFEATED_BY_OTHER:
                        defaultGreet(text);

                        if (hasDefeatedInitialFleet) {
                            stage = pickRandomEventStart();
                        } else {
                            stage = AgentEventStages.IDLE;
                        }

                        if (fleetIntel != null && !fleetIntel.isEnded()) {
                            fleetIntel.endImmediately();
                            fleetIntel = null;
                        }

                        text.addPara(
                                person.getName().getLast() + " looks concerned." +
                                " \"Intelligence from above has lost track of those stolen elite ships you had the bounty on," +
                                " so we can only hope that you can be more punctual if there is another" +
                                " security breach we have to recover.\""
                        );

                        if (hasDefeatedInitialFleet) {
                            reportNoFurtherWork(text, options);
                        } else {
                            options.addOption("Make your apologies and leave.", OptionDialog.LEAVE);
                        }
                        break;
                    case GIVE_SUBMARKET_QUEST:
                        defaultGreet(text);

                        camp = getHiddenSubmarketMarket();
                        if (camp == null) {
                            reportNoFurtherWork(text, options);
                        } else {
                            long previousTimestamp = Global.getSector().getMemoryWithoutUpdate().getLong(QUEST_TIME_TRACKER_KEY);
                            float months = Global.getSector().getClock().getElapsedDaysSince(previousTimestamp) / 30f;

                            if (months > 6f) {
                                text.addPara("\"I haven't forgotten that work you were able do for me a while ago," +
                                        " which is why I'm offering you another job now, if you're interested.\""
                                );
                            } else {
                                text.addPara("\"With that excellent work in recovering those special assets recently, I have" +
                                        " an opportunity for you to help me out again, if you're interested.\""
                                );
                            }
                            if (!hasFoundInitialSubmarket) {
                                LabelAPI p1 = text.addPara(person.getName().getLast() + " shifts in his seat before continuing." +
                                        " \"As you may have guessed from the unredacted intel notes for those stolen warships, they were" +
                                        " officially designated as property of the Special Operations Division of the Pearson Exotronics Navy." +
                                        " Naturally, some know of the Division's name and reputation, but I'll give you a summary" +
                                        " of who my employers are and their relevance to this job.\""
                                );

                                p1.setHighlight("Special Operations Division", "Pearson Exotronics Navy");
                                p1.setHighlightColors(person.getFaction().getBaseUIColor(), Global.getSector().getFaction("pearson_exotronics").getBaseUIColor());
                            }

                            options.addOption("Continue.", OptionDialog.DELIVER_PARCEL_TO_HIDDEN_CAMP);
                        }
                        break;
                    case SUBMARKET_QUEST_ACTIVE:
                        defaultGreet(text);

                        text.addPara("\"Found our boys yet?\"");

                        options.addOption("\"Still looking.\"", OptionDialog.LEAVE);
                        break;
                    case SUBMARKET_QUEST_COMPLETED:
                        defaultGreet(text);

                        options.addOption("Describe your success.", OptionDialog.DELIVERY_SUCCESSFUL);
                        break;
                    case IDLE:
                        defaultGreet(text);
                        reportNoFurtherWork(text, options);
                        break;
                }
                break;
            case INTRO_TO_FLEET_QUEST: //character introduction
                stage = AgentEventStages.GIVE_FLEET_BOUNTY;
                bountyReward = getSpecialBountyReward();

                text.addPara(
                        "The " + getManOrWoman() +
                        " appraises you for a second, tilting " +
                        getHisOrHer() +
                        " head, then grimaces. \"My name is " + person.getName().getFullName() +
                        ".\", " + getHeOrShe() + " says, while you get the feeling that isn't " + getHisOrHer() + " real name." +
                        " \"As you might've guessed, I have employers that pay me to deal with certain sensitive operations.\", " +
                        getHeOrShe() + " says, delicately stressing the nature of such operations."
                );

                String fleetInfoStr =
                        Misc.ucFirst(getHeOrShe()) + " pauses for a second, absorbing some information over the net. \"Captain " +
                        Global.getSector().getPlayerPerson().getNameString() +
                        ", right?";

                int crew = 0;
                for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                    crew += member.getCrewComposition().getCrewInt();
                }

                if (Global.getSector().getPlayerFleet().isTransponderOn()) {
                    fleetInfoStr += " Dockyard management intranet says you're orbiting with " + crew + " personnel, a considerable active force.\"";
                } else {
                    fleetInfoStr += " Intel indicates you're orbiting with " + crew + " personnel, a considerable active force." +
                    " Don't worry, no patrols have been alerted, even though you're technically breaking port law.\" " +
                    Misc.ucFirst(getHeOrShe()) + " winks exaggeratedly.";
                }
                text.addPara(fleetInfoStr);

                if (isCommissioned()) {
                    text.addPara(
                            "\"Since you're commissioned with the Navy, any expeditions in my employer's interests are sanctioned by the Corporation.\"" +
                            " A roguish gleam enters " + getHisOrHer() + "eye, and he pulls out a battered datapad." +
                            " \"With those legalities out of the way, we can talk business.\""
                    );

                    options.addOption("Look at the information on the datapad.", OptionDialog.GIVE_FLEET_QUEST);
                    //options.addOption("Politely refuse " + person.getName().getLast() + "'s offer and leave.", OptionDialog.LEAVE);
                } else {
                    text.addPara(
                            "\"Unfortunately, since you don't have a formal relationship with the Navy, " +
                            "any expeditions in my employer's interests are not sanctioned and if necessary will result in official denouncement.\"" +
                            " A roguish gleam enters " + getHisOrHer() + " eye, and he pulls out a battered datapad." +
                            " \"Regardless of such bureaucracy, I reckon we can still talk business.\""
                    );

                    options.addOption("Look at the information on the datapad.", OptionDialog.GIVE_FLEET_QUEST);
                }
                break;
            case GIVE_FLEET_QUEST:
                fleetIntel = new PSE_SODBountyFleetIntel((int) bountyReward, person);
                Global.getSector().addScript(fleetIntel);
                fleetIntel.getFleet().addEventListener(fleetIntel);

                FactionAPI faction = fleetIntel.getBountyFaction();
                SectorEntityToken location = fleetIntel.getHideoutLocation();


                String str1 = "\"A rather unfortunate lapse in dockyard security led to an agent from " +
                        faction.getDisplayNameWithArticle() + " stealing plans for experimental systems upgrades from our elite corps." +
                        " Naturally, you can understand why this must remain clandestine.\" " + Misc.ucFirst(getHeOrShe()) + " catches your eye, glancing sidelong.";

                String str2 = " \"I'm offering you " + (int) bountyReward + " credits to resolve this situation.";

                String str3 = "Intelligence thinks the criminal is hiding out" +
                        " in the " + location.getStarSystem().getNameWithTypeIfNebula() + " system." +
                        " You are entitled to any post-battle salvage, of course.\"";
                text.addPara(str1);
                text.highlightInLastPara(faction.getColor(), faction.getDisplayNameWithArticle());

                text.addPara(str2);
                text.highlightInLastPara((int) bountyReward + "");

                text.addPara(str3);
                text.highlightInLastPara(location.getStarSystem().getNameWithTypeIfNebula());

                options.addOption("\"I accept.\"", OptionDialog.FLEET_QUEST_ACCEPTED);
                options.addOption("Politely refuse " + person.getName().getLast() + "'s offer and leave.", OptionDialog.LEAVE_DECLINE_FLEET_QUEST);
                break;
            case FLEET_QUEST_ACCEPTED:
                Global.getSector().getIntelManager().addIntel(fleetIntel, false);
                stage = AgentEventStages.FLEET_BOUNTY_ACTIVE;

                text.addPara(
                        "The agent grins and shakes your hand. \"Good stuff! I'll see you round, I hope.\""
                );

                options.addOption("Make a note of the bounty fleet data and leave.", OptionDialog.LEAVE);
                break;
            case FLEET_DEFEATED:
                hasDefeatedInitialFleet = true;

                if (fleetIntel != null) {
                    if (!fleetIntel.isEnded()) {
                        fleetIntel.endImmediately();
                    }

                    fleetIntel = null;
                }

                //Global.getSector().getMemoryWithoutUpdate().set(PSE_SODBountyFleetIntel.SPECIAL_BOUNTY_KEY, );

                stage = AgentEventStages.IDLE;

                text.addPara(
                        person.getName().getLast() +
                                " drums his fingers on the plasteel countertop excitedly while scrolling through a spreadsheet on " + getHisOrHer() + " datapad." +
                                " \"Intel came in yesterday from HQ, that agent was planning to rendezvous with a Tri-Tach corporate minion, so we're grateful you were able to sort that out." +
                                " " + (int) bountyReward + " credits have been transferred to your account.\""
                );
                text.highlightInLastPara((int) bountyReward + "");

                payPlayer(text);

                options.addOption("Leave.", OptionDialog.LEAVE);
                break;
            case DELIVER_PARCEL_TO_HIDDEN_CAMP:
                CommodityOnMarketAPI commodity = pickCommodity();
                String commodityName = Global.getSector().getEconomy().getCommoditySpec(commodity.getCommodity().getId()).getName();

                int quantity = pickDeliveryQuantity(commodity.getCommodity().getId());
                float reward = pickDeliveryReward(commodity.getCommodity().getId(), quantity);

                campIntel = new PSE_SODCampDeliveryIntel(camp, commodity, quantity, reward);

                if (!hasFoundInitialSubmarket) {
                    LabelAPI p2 = text.addPara("\"As the name implies, the Division act as military intelligence" +
                            " and execute covert operations throughout the sector in the interests of the Corporation." +
                            " Since you've proven yourself trustworthy, I have authorisation to offer you a contract" +
                            " delivering some sensitive goods to one such operation.\""
                    );
                }

                text.addPara("The agent hands you a secure datachip with company markings." +
                        " \"This has the contract data. Take a look and decide if you'll accept the offer.\" " +
                        "While you read, " +
                        person.getName().getLast() + " sits back and folds his arms, absorbed in some other stream of data" +
                        " flowing through his net implant."
                );

                text.addPara("The delivery contract requires you to deliver " + quantity +  " " + commodityName +
                        " to " + camp.getAssociatedMarket().getName() + ". The camp is expected to remain for " + camp.getCurrentLifetime() +
                        " months, in which time the delivery must be completed."
                );
                text.highlightInLastPara(Misc.getHighlightColor(), quantity + "", commodityName, camp.getAssociatedMarket().getName(), camp.getCurrentLifetime() + "");

                String where = "located in hyperspace,";
                if (camp.getAssociatedMarket().getStarSystem() != null) {
                    where = "located in the " + camp.getAssociatedMarket().getStarSystem().getNameWithLowercaseType() + ", which is";
                }
                String str = "You recall that " + camp.getAssociatedMarket().getName() + " is under " + camp.getAssociatedMarket().getFaction().getDisplayName() +
                        " control, and " + where + " " + (int) Misc.getDistanceToPlayerLY(camp.getAssociatedMarket().getLocationInHyperspace()) + " light-years away. ";
                LabelAPI p3 = text.addPara(str);

                if (camp.getAssociatedMarket().getStarSystem() != null) {
                    p3.setHighlight(
                            camp.getAssociatedMarket().getName(),
                            camp.getAssociatedMarket().getFaction().getDisplayName(),
                            camp.getAssociatedMarket().getStarSystem().getNameWithLowercaseType(),
                            (int) Misc.getDistanceToPlayerLY(camp.getAssociatedMarket().getLocationInHyperspace()) + ""
                    );

                    p3.setHighlightColors(
                            Misc.getHighlightColor(),
                            camp.getAssociatedMarket().getFaction().getBaseUIColor(),
                            Misc.getHighlightColor(),
                            Misc.getHighlightColor()
                    );
                } else {
                    p3.setHighlight(
                            camp.getAssociatedMarket().getName(),
                            camp.getAssociatedMarket().getFaction().getDisplayName(),
                            (int) Misc.getDistanceToPlayerLY(camp.getAssociatedMarket().getLocationInHyperspace()) + ""
                    );

                    p3.setHighlightColors(
                            Misc.getHighlightColor(),
                            camp.getAssociatedMarket().getFaction().getBaseUIColor(),
                            Misc.getHighlightColor()
                    );
                }

                options.addOption("Accept the contract.", OptionDialog.ACCEPT_DELIVERY);
                options.addOption("Reject " + person.getName().getLast() + "'s offer and leave.", OptionDialog.LEAVE_DECLINE_DELIVERY);
                break;
            case ACCEPT_DELIVERY:
                Global.getSector().getMemoryWithoutUpdate().set(PSE_SODCampDeliveryIntel.DELIVERED_CAMP_KEY, null);

                Global.getSector().getIntelManager().addIntel(campIntel, false);

                stage = AgentEventStages.SUBMARKET_QUEST_ACTIVE;

                Global.getSector().getPlayerFleet().getCargo().addCommodity(campIntel.getCommodityId(), campIntel.getQuantity());
                AddRemoveCommodity.addCommodityGainText(campIntel.getCommodityId(), campIntel.getQuantity(), text);

                text.addPara("\"Good stuff, I'll see you 'round!\"");

                options.addOption("Leave.", OptionDialog.LEAVE);
                break;
            case DELIVERY_SUCCESSFUL:
                stage = AgentEventStages.IDLE;
                hasFoundInitialSubmarket = true;

                text.addPara(
                        person.getName().getLast() + " is pleased by the news. \"Excellent, I knew I could count on you!" +
                        " What they needed that cargo for, I have no idea, but it means more opportunities for you." +
                        " I don't have any more work at the moment, but if you come back in a week or two, I could have something new.\""
                );

                options.addOption("Leave.", OptionDialog.LEAVE);
                break;
            case LEAVE:
                noContinue = true;
                done = true;
                break;
            case LEAVE_DECLINE_FLEET_QUEST:
                text.addPara("\"That's too bad. HQ will find someone else to deal with it, but there's always opportunity for starfarers here.\"");
                stage = AgentEventStages.IDLE;
                fleetIntel.endImmediately();
                fleetIntel = null;

                noContinue = true;
                done = true;
                break;
            case LEAVE_DECLINE_DELIVERY:
                text.addPara("\"That's a pain, but I'll find someone else to do it if you're no longer interested.");
                stage = AgentEventStages.IDLE;
                campIntel.endImmediately();
                campIntel = null;

                noContinue = true;
                done = true;
                break;
            default:
                break;
        }
    }

    private void reportNoFurtherWork(TextPanelAPI text, OptionPanelAPI options) {
        text.addPara("\"Sorry mate, no further work available. Check back again sometime and I'll probably have something for you.\"");
        options.addOption("Leave after some small talk.", OptionDialog.LEAVE);
    }

    @Override
    protected PersonAPI createPerson() {
        if (person != null) {
            return person;
        }
        PersonAPI person = super.createPerson();
        person.getName().setFirst(FIRST_NAME);
        person.getName().setLast(LAST_NAME);
        return person;
    }

    @Override
    protected void regen(MarketAPI market) {
        if (this.market == market) {
            return;
        }
        this.market = market;
        person = createPerson();
        random = new Random(seed + market.getId().hashCode());
    }

    private void payPlayer(TextPanelAPI text) {
        Global.getSector().getPlayerFleet().getCargo().getCredits().add(bountyReward);
        AddRemoveCommodity.addCreditsGainText((int) bountyReward, text);
    }

    private String getRandomPromptLine() {
        long time = Global.getSector().getClock().getTimestamp();
        int index = new Random(time).nextInt(prompts.size());
        return prompts.get(index).replace("$NAME", person.getNameString());
    }

    private AgentEventStages pickRandomEventStart() {
        Random r = new Random(person.hashCode());
        return STARTING_STAGES.get(r.nextInt(STARTING_STAGES.size()));

        /*if (isCommissioned()) {
            return STARTING_STAGES.get(r.nextInt(STARTING_STAGES.size()));
        } else {
            return AgentEventStages.GIVE_FLEET_BOUNTY;
        }*/
    }

    private void defaultGreet(TextPanelAPI text) {
        text.addPara("The scarred agent waves at you from the bar counter and shouts you a drink.");
    }

    private boolean isCommissioned() {
        return Misc.getCommissionFaction() != null && Misc.getCommissionFactionId().equals("pearson_exotronics");
    }

    private float getSpecialBountyReward() {
        float frac = Global.getSector().getPlayerStats().getLevel() / 50f;
        return frac * FLEET_BOUNTY_REWARD_BASE + 90000f;
    }

    @SuppressWarnings("unchecked")
    private PSE_SODCamp getHiddenSubmarketMarket() {
        List<PSE_SODCamp> camps = (List<PSE_SODCamp>) Global.getSector().getPersistentData().get(PSE_SODCampEventListener.SOD_CAMPS_DATA_KEY);
        for (PSE_SODCamp camp : camps) {
            if (!camp.isDiscovered()) {
                return camp;
            }
        }
        return null;
    }

    private CommodityOnMarketAPI pickCommodity() {
        MarketAPI market = camp.getAssociatedMarket();
        List<CommodityOnMarketAPI> suitable = new ArrayList<>();
        for (String id : Global.getSector().getEconomy().getAllCommodityIds()) {
            CommodityOnMarketAPI commodity = market.getCommodityData(id);

            CommoditySpecAPI spec = Global.getSector().getEconomy().getCommoditySpec(id);
            if (spec.hasTag("nonecon")) continue; //prevents ai core and survey data distribution

            if (commodity != null && market.isIllegal(id)) {
                suitable.add(commodity);
            }
        }

        if (suitable.isEmpty()) {
            return market.getCommodityData(Commodities.HAND_WEAPONS);
        } else {
            Random r = new Random(person.hashCode());

            return suitable.get(r.nextInt(suitable.size()));
        }
    }

    private float pickDeliveryReward(String id, int quantity) {
        float base = Global.getSector().getEconomy().getCommoditySpec(id).getBasePrice();
        //float frac = (Global.getSector().getPlayerStats().getLevel() / 50f) + 0.25f;
        return base * quantity * 1.25f;
    }

    private int pickDeliveryQuantity(String id) {
        CommoditySpecAPI spec = Global.getSector().getEconomy().getCommoditySpec(id);
        float value = spec.getBasePrice();
        float frac = Global.getSector().getPlayerStats().getLevel() / 50f;
        float min = 20f;
        Random r = new Random(person.hashCode());

        float inf = 300f / (value + 100f);

        return (int) ((inf * ((50f * frac) + 80f * r.nextFloat())) + min);
    }

    @Override
    protected String getPersonPortrait() {
        return AGENT_PORTRAIT;
    }

    @Override
    protected String getPersonFaction() {
        return "pearson_division";
    }

    @Override
    protected String getPersonRank() {
        return Ranks.AGENT;
    }

    @Override
    protected FullName.Gender getPersonGender() {
        return GENDER;
    }

    @Override
    protected String getPersonPost() {
        return Ranks.AGENT;
    }

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        return false;
//        if (!market.getFaction().getId().contentEquals(Factions.INDEPENDENT)) {
//            return false;
//        }
//        if (!market.getName().contentEquals("Iuchar")) {
//            return false;
//        }
//        return true;
    }
}
