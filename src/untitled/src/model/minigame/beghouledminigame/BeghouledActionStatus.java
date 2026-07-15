package model.minigame.beghouledminigame;

public enum BeghouledActionStatus {
    SUCCESS,
    STAGE_WON,
    GAME_WON,
    GAME_LOST,

    INVALID_STAGE,
    STAGE_LOCKED,
    NOT_STARTED,
    GAME_COMPLETED,

    INVALID_POSITION,
    INVALID_SWAP,

    UPGRADE_NOT_FOUND,
    NOT_ENOUGH_SUN,
    NO_PLANTS_TO_UPGRADE,

    INVALID_TICK_COUNT
}