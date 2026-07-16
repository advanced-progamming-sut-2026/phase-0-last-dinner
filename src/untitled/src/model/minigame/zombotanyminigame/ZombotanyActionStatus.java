package model.minigame.zombotanyminigame;

public enum ZombotanyActionStatus {
    SUCCESS,
    STAGE_WON,
    GAME_WON,
    GAME_LOST,

    INVALID_STAGE,
    STAGE_LOCKED,
    NOT_STARTED,
    GAME_COMPLETED,

    INVALID_PLANT,
    INVALID_POSITION,
    NOT_ENOUGH_SUN,
    CANNOT_PLANT,

    NO_SUN_AT_POSITION,
    NO_PLANT_FOOD,
    CANNOT_USE_PLANT_FOOD,

    INVALID_TICK_COUNT
}