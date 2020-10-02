package data.scripts.campaign;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

public class PSE_SODCamp {
    private boolean isDiscovered = false;
    private int maxLifetime;
    private int currentLifetime;
    private MarketAPI associatedMarket;
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

    int getCurrentLifetime() {
        return currentLifetime;
    }

    public int getMaxLifetime() {
        return maxLifetime;
    }

    MarketAPI getAssociatedMarket() {
        return associatedMarket;
    }
}
