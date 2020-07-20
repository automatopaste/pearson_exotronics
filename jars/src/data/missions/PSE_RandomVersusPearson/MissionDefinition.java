package data.missions.PSE_RandomVersusPearson;

import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import data.missions.PSE_BaseRandomPSEMissionDefinition;

public class MissionDefinition extends PSE_BaseRandomPSEMissionDefinition {
    @Override
    public void defineMission(MissionDefinitionAPI api) {
        chooseFactions(null, "pearson_exotronics");
        super.defineMission(api);
    }
}
