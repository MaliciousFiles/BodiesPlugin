package io.github.maliciousfiles.bodiesplugin.util;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public class CustomZombie extends Zombie {
    public CustomZombie(Level world) {
        super(world);
    }

    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        this.goalSelector.addGoal(1, new FloatGoal(this));
    }

    @Override
    public boolean convertsInWater() { return false; }

    public void setNoAi(boolean ignored) { }
    public void setLeftHanded(boolean ignored) { }
    public void setAggressive(boolean ignore) { }
}
