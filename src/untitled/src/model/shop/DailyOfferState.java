package model.shop;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor //برا جیسان
public class DailyOfferState {
    private String offerId;
    private LocalDate offerDate;
    private DailyOffer offer;
    private String plantName;
    private boolean purchased;

    DailyOfferState(String offerId, LocalDate offerDate, String plantName, DailyOffer offer) {
        this.offerId = offerId;
        this.offerDate = offerDate;
        this.plantName = plantName;
        this.offer = offer;
        this.purchased = false;
    }

    public boolean canPurchase(LocalDate currentDate) {
        if(offerDate == null)
            return false;
        if(currentDate == null)
            return false;
        if(!isStateForDate(currentDate))
            return false;
        if(offer == null)
            return false;
        if(plantName == null)
            return false;
        return !purchased;
    }

    public boolean markAsPurchased() {
        if(purchased)
            return false;
        purchased = true;
        return true;
    }

    public boolean isStateForDate(LocalDate date) {
        if(date == null)
            return false;
        if(offerDate == null)
            return false;

        return date.equals(offerDate);
    }
}
