package model.mechanism;

public enum SunType {
    NORMAL(25),
    SPECIAL(100),
    RADIOACTIVE(25),
    PLANT_PRODUCED(25);

    private final int value;
    SunType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
