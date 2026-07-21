package model.shop;

import lombok.Getter;
import lombok.NoArgsConstructor;
import model.Plant;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Getter
@NoArgsConstructor
public class Shop {

    private DailyOfferState dailyOfferState;

    public List<PermanentStuff> getPermanentItems() {
        return Collections.unmodifiableList(
                Arrays.asList(PermanentStuff.values())
        );
    }

    public PermanentStuff findPermanentItemById(String itemId) {
        String normalizedId = normalizeItemId(itemId);

        if (normalizedId.isEmpty()) return null;


        for (PermanentStuff item : PermanentStuff.values()) {
            if (item.getItemId().equalsIgnoreCase(normalizedId)) return item;
        }

        return null;
    }

    public DailyOfferState getOrRefreshDailyOffer(LocalDate currentDate, List<Plant> unlockedPlants, Random random) {
        if (currentDate == null) return null;

        if (dailyOfferState != null && dailyOfferState.isStateForDate(currentDate)) {

            return dailyOfferState;
        }

        List<Plant> eligiblePlants = getEligiblePlants(unlockedPlants);

        if (eligiblePlants.isEmpty()) {
            dailyOfferState = null;
            return null;
        }

        if (random == null) random = new Random();


        Plant selectedPlant = eligiblePlants.get(random.nextInt(eligiblePlants.size()));

        String plantName = selectedPlant.getName().trim();

        String offerId = createDailyOfferId(currentDate, plantName);

        dailyOfferState = new DailyOfferState(offerId, currentDate, plantName, DailyOffer.DISCOUNTED_SEED_PACKET);

        return dailyOfferState;
    }

    private List<Plant> getEligiblePlants(List<Plant> unlockedPlants) {
        List<Plant> eligiblePlants = new ArrayList<>();

        if (unlockedPlants == null) {
            return eligiblePlants;
        }

        for (Plant plant : unlockedPlants) {
            if (plant == null || plant.getName() == null || plant.getName().trim().isEmpty()) {

                continue;
            }

            eligiblePlants.add(plant);
        }

        return eligiblePlants;
    }

    private String createDailyOfferId(LocalDate date, String plantName) {
        return "daily-" + date + "-" + normalizeIdPart(plantName);
    }

    private String normalizeItemId(String itemId) {
        if (itemId == null) {
            return "";
        }

        return itemId.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
    }

    private String normalizeIdPart(String value) {
        if (value == null) {
            return "unknown";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT).
                replaceAll("[^a-z0-9]+", "-").
                replaceAll("^-+|-+$", "");

        return normalized.isEmpty() ? "unknown" : normalized;
    }
}
