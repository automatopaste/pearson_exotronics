package data.scripts.campaign.intel.bar.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public class PSE_SpecialAgentBarEventCreator extends BaseBarEventCreator {

    @Override
    public PortsideBarEvent createBarEvent() {
        Global.getLogger(this.getClass()).info("Creating bar event");
        return new PSE_SpecialAgentBarEvent();
    }

    @Override
    public boolean isPriority() {
        return true;
    }

    @Override
    public float getBarEventActiveDuration() {
        return Float.MAX_VALUE;
    }

    @Override
    public float getBarEventTimeoutDuration()
    {
        return 0f;
    }

    @Override
    public float getBarEventAcceptedTimeoutDuration()
    {
        return 0f;
    }
}
