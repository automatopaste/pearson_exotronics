package data.missions.PSE_PearsonVersusRandom;

import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import data.missions.PSE_BaseRandomPSEMissionDefinition;

public class MissionDefinition extends PSE_BaseRandomPSEMissionDefinition {
    @Override
    public void defineMission(MissionDefinitionAPI api) {
        chooseFactions("pearson_exotronics", null);
        super.defineMission(api);
    }
}
