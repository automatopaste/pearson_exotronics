package data.scripts.campaign;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.scripts.campaign.intel.PSE_SODCampLocationBreadcrumbIntel;

public class PSE_SODCamp {
    private boolean isDiscovered = false;
    private int maxLifetime;
    private int currentLifetime;
    private MarketAPI associatedMarket;
    private PSE_SODCampLocationBreadcrumbIntel intel;

    PSE_SODCamp(MarketAPI market, int lifetimeTicks) {
        associatedMarket = market;
        maxLifetime = lifetimeTicks;
        currentLifetime = maxLifetime;
    }

    public boolean isDiscovered() {
        return isDiscovered;
    }

    void setDiscovered(boolean discovered) {
        isDiscovered = discovered;
    }

    void setCurrentLifetime(int currentLifetime) {
        this.currentLifetime = currentLifetime;
    }

    public int getCurrentLifetime() {
        return currentLifetime;
    }

    public int getMaxLifetime() {
        return maxLifetime;
    }

    public MarketAPI getAssociatedMarket() {
        return associatedMarket;
    }

    public PSE_SODCampLocationBreadcrumbIntel getIntel() {
        return intel;
    }

    public void setIntel(PSE_SODCampLocationBreadcrumbIntel intel) {
        this.intel = intel;
    }
}
