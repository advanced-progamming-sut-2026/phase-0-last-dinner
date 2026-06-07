package model;

import java.time.LocalDate;

public class DailyOfferState {
    private String offerId;
    private LocalDate offerDate;
    private DailyOffer offer;
    private String plantName;
    private boolean purchased;

    public boolean canPurchase(LocalDate currentDate) {
        return false;
    }

    public void markAsPurchased() {
    }
}
