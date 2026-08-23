package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieFactory;

public class GargantuarBehavior implements ZombieBehavior {
    private static final int TICKS_PER_SECOND = 10;
    private static final int SMASH_DAMAGE = 1500;
    private static final int SMASH_DURATION_TICKS = 2 * TICKS_PER_SECOND;
    // zarbeye choob kami bad az nimeye clip-e smash be zamin mirese.
    private static final int SMASH_IMPACT_TICK = 11;

    private static final int THROW_WINDUP_TICKS = 1 * TICKS_PER_SECOND;
    private static final int IMP_FLIGHT_TICKS = 15;
    private static final double IMP_TARGET_COLUMN = 2.0d;
    private static final double IMP_SPAWN_X_OFFSET_TILES = -0.70d;
    private static final double IMP_SPAWN_Z = 115.0d;
    private static final double IMP_APEX_Z = 250.0d;

    private ZombieDefinition impDefinition;
    private ZombieFactory zombieFactory;
    private double throwHealthThreshold;
    private boolean impThrown;
    private int smashDamage = SMASH_DAMAGE;

    private boolean throwingImp;
    private int throwTicksRemaining;

    private boolean smashing;
    private int smashTicksRemaining;
    private boolean smashDamageApplied;
    private Plant smashTarget;
    private long smashImpactSerial;

    public GargantuarBehavior(
            ZombieDefinition impDefinition,
            ZombieFactory zombieFactory,
            double throwHealthThreshold
    ) {
        this.impDefinition = impDefinition;
        this.zombieFactory = zombieFactory;
        this.throwHealthThreshold = throwHealthThreshold;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (zombie == null || zombie.isDead()) {
            return;
        }

        if (this.throwingImp) {
            this.tickImpThrow(zombie, board);
            return;
        }

        if (this.smashing) {
            this.tickSmash(zombie, board);
            return;
        }

        if (!this.impThrown
                && zombie.getHealth() <= zombie.getMaximumHealth() * this.throwHealthThreshold
                && this.canThrowImpFrom(zombie)) {
            this.beginImpThrow(zombie);
            this.tickImpThrow(zombie, board);
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (zombie == null || zombie.isDead() || plant == null || plant.isDead()
                || board == null || board.getCombatSystem() == null) {
            return;
        }

        if (this.throwingImp) {
            zombie.setAttacking(false);
            return;
        }

        if (this.smashing) {
            zombie.setAttacking(true);
            return;
        }

        this.smashing = true;
        this.smashTicksRemaining = SMASH_DURATION_TICKS;
        this.smashDamageApplied = false;
        this.smashTarget = plant;
        zombie.setAttacking(true);
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        if (!this.impThrown && !this.throwingImp && !this.smashing && this.canThrowImpFrom(zombie)) {
            this.beginImpThrow(zombie);
        }
    }

    @Override
    public void multiplyDamage(double multiplier) {
        if (multiplier > 0d) {
            this.smashDamage = Math.max(1, (int) Math.round(SMASH_DAMAGE * multiplier));
        }
    }

    @Override
    public boolean canMove(Zombie zombie, Board board) {
        return !this.throwingImp && !this.smashing;
    }

    @Override
    public boolean canAttackPlant(Zombie zombie, Plant plant, Board board) {
        // Gargantuar az bite DPS-e peyvaste estefade nemikone. BasicZombieBehavior az masire
        // composite attack(...) ro seda mizane va in behavior yek cycle-e zamanbandi-shode smash ro shoroo mikone.
        return false;
    }

    public boolean isImpThrown() {
        return this.impThrown;
    }

    public boolean isThrowingImp() {
        return this.throwingImp;
    }

    public boolean isSmashing() {
        return this.smashing;
    }

    public long getSmashImpactSerial() {
        return this.smashImpactSerial;
    }

    public int getSmashTicksRemaining() {
        return this.smashTicksRemaining;
    }

    public int getThrowTicksRemaining() {
        return this.throwTicksRemaining;
    }

    public Zombie throwImp(Zombie zombie, Board board) {
        if (this.impThrown || this.throwingImp || this.smashing || !this.canThrowImpFrom(zombie)) {
            return null;
        }
        this.beginImpThrow(zombie);
        return null;
    }

    private void beginImpThrow(Zombie zombie) {
        this.throwingImp = true;
        this.throwTicksRemaining = THROW_WINDUP_TICKS;
        if (zombie != null) {
            zombie.setAttacking(false);
        }
    }

    private void tickImpThrow(Zombie zombie, Board board) {
        if (this.throwTicksRemaining > 0) {
            this.throwTicksRemaining--;
        }

        if (this.throwTicksRemaining > 0) {
            return;
        }

        this.releaseImp(zombie, board);
        this.throwingImp = false;
        if (zombie != null) {
            zombie.setAttacking(false);
        }
    }

    private Zombie releaseImp(Zombie zombie, Board board) {
        if (this.impThrown || zombie == null || board == null
                || this.zombieFactory == null || this.impDefinition == null
                || zombie.getPosition() == null) {
            return null;
        }

        int row = zombie.getPosition().getY();
        double startX = Math.max(
                IMP_TARGET_COLUMN + 0.20d,
                zombie.getExactX() + IMP_SPAWN_X_OFFSET_TILES
        );
        int spawnColumn = Math.max(0, Math.min(8, (int) Math.round(startX)));
        Position spawnPosition = new Position(spawnColumn, row);
        Zombie imp = this.zombieFactory.create(this.impDefinition, spawnPosition);
        if (imp == null) {
            return null;
        }

        board.addZombie(imp, spawnPosition);
        imp.startFlight(startX, IMP_SPAWN_Z, IMP_TARGET_COLUMN, IMP_APEX_Z, IMP_FLIGHT_TICKS);

        if (zombie.getWave() != null) {
            zombie.getWave().addZombie(imp);
        }

        this.impThrown = true;
        return imp;
    }

    private void tickSmash(Zombie zombie, Board board) {
        if (this.smashTicksRemaining > 0) {
            this.smashTicksRemaining--;
        }
        int elapsedTicks = SMASH_DURATION_TICKS - this.smashTicksRemaining;
        if (!this.smashDamageApplied && elapsedTicks >= SMASH_IMPACT_TICK) {
            this.smashDamageApplied = true;
            if (this.isValidSmashTarget(zombie, this.smashTarget)
                    && board != null && board.getCombatSystem() != null) {
                board.getCombatSystem().applyDamageToPlant(this.smashTarget, this.smashDamage);
                this.smashImpactSerial++;
            }
        }

        if (this.smashTicksRemaining <= 0) {
            this.smashing = false;
            this.smashDamageApplied = false;
            this.smashTarget = null;
            if (zombie != null) {
                zombie.setAttacking(false);
            }
        } else if (zombie != null) {
            zombie.setAttacking(true);
        }
    }

    private boolean isValidSmashTarget(Zombie zombie, Plant plant) {
        if (zombie == null || zombie.getPosition() == null || plant == null
                || plant.isDead() || plant.getPosition() == null) {
            return false;
        }
        if (zombie.getPosition().getY() != plant.getPosition().getY()) {
            return false;
        }
        return zombie.getPosition().getX() - plant.getPosition().getX() >= 0
                && zombie.getPosition().getX() - plant.getPosition().getX() <= 1;
    }

    private boolean canThrowImpFrom(Zombie zombie) {
        return zombie != null && !zombie.isDead() && zombie.getPosition() != null
                && this.zombieFactory != null && this.impDefinition != null
                && zombie.getExactX() > IMP_TARGET_COLUMN + 0.5d;
    }
}
