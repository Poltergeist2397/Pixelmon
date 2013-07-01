package pixelmon.battles.attacks.specialAttacks.basic;

import java.util.ArrayList;

import net.minecraft.util.DamageSource;
import pixelmon.battles.attacks.Attack;
import pixelmon.battles.attacks.Value;
import pixelmon.comm.ChatHandler;
import pixelmon.entities.pixelmon.EntityPixelmon;

public class JumpKick extends SpecialAttackBase {

	public JumpKick() {
		super(ApplyStage.During, false);
	}

	@Override
	public boolean ApplyEffect(EntityPixelmon user, EntityPixelmon target, Attack a, double crit, ArrayList<String> attackList, ArrayList<String> targetAttackList) {
		System.out.println("Runs applyeffect");
		return false;
	}

	@Override
	public void ApplyMissEffect(EntityPixelmon user, EntityPixelmon target) throws Exception {
		user.attackEntityFrom(DamageSource.causeMobDamage(user), user.getMaxHealth() / 2);
		ChatHandler.sendBattleMessage(user.getOwner(), target.getOwner(), user.getNickname() + " kept on going and crashed!");
	}
}
