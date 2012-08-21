package pixelmon.entities.pokeballs;

import java.util.Random;


import pixelmon.Pixelmon;
import pixelmon.RandomHelper;
import pixelmon.comm.ChatHandler;
import pixelmon.entities.EntityTrainer;
import pixelmon.entities.pixelmon.helpers.IHaveHelper;
import pixelmon.entities.pixelmon.helpers.PixelmonEntityHelper;
import pixelmon.enums.EnumPokeballs;
import pixelmon.items.ItemPokeBall;
import pixelmon.storage.PixelmonStorage;
import pixelmon.storage.PokeballManager;
import net.minecraft.src.Block;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.EntityThrowable;
import net.minecraft.src.EnumMovingObjectType;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Material;
import net.minecraft.src.MathHelper;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.Vec3;
import net.minecraft.src.World;

public class EntityPokeBall extends EntityThrowable {
	public int shakePokeball;
	private EntityLiving thrower;
	private PixelmonEntityHelper p;
	private boolean isWaiting;
	private int waitTime;
	private boolean canCatch = false;
	private PixelmonEntityHelper pixelmon;
	private boolean isEmpty;
	private float endRotationYaw = 0;
	public boolean dropItem;

	public EntityPokeBall(World world) {
		super(world);
		dataWatcher.addObject(10, EnumPokeballs.PokeBall.getIndex());
	}

	public EntityPokeBall(World world, EntityLiving entityliving, EnumPokeballs type, boolean dropItem) {
		super(world, entityliving);
		thrower = entityliving;
		dataWatcher.addObject(10, type.getIndex());
		isEmpty = true;
		this.dropItem = dropItem;
	}

	public EntityPokeBall(World world, EntityLiving entityliving, PixelmonEntityHelper e, EnumPokeballs type) {
		super(world, entityliving);
		thrower = entityliving;
		endRotationYaw = entityliving.rotationYawHead;
		pixelmon = e;
		dataWatcher.addObject(10, type.getIndex());
		isEmpty = false;
		float speed = 0.3f;
		this.setLocationAndAngles(entityliving.posX, entityliving.posY + (double) entityliving.getEyeHeight(), entityliving.posZ, entityliving.rotationYaw, entityliving.rotationPitch);
		this.posX -= (double) (MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI) * 0.16F);
		this.posY -= 0.10000000149011612D;
		this.posZ -= (double) (MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI) * 0.16F);
		this.setPosition(this.posX, this.posY, this.posZ);
		this.yOffset = 0.0F;
		this.motionX = (double) (-MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI)) * 0.8;
		this.motionZ = (double) (MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI)) * 0.8;
		this.motionY = (double) (-MathHelper.sin(this.rotationPitch / 180.0F * (float) Math.PI)) * 0.8;
	}

	public void init() {
	}

	@Override
	protected void onImpact(MovingObjectPosition movingobjectposition) {

		if (!isEmpty) {
			if (movingobjectposition != null && !worldObj.isRemote) {
				ItemPokeBall.ballTimer = 0;
				if (pixelmon != null) {
					if(movingobjectposition.typeOfHit == EnumMovingObjectType.TILE)
					{
						Material mat = worldObj.getBlockMaterial(movingobjectposition.blockX, movingobjectposition.blockY, movingobjectposition.blockZ);
						if(mat != null && mat.isSolid())
						{
							pixelmon.setLocationAndAngles(prevPosX, prevPosY, prevPosZ, rotationYaw, 0.0F);
						}
						else return;
					}
					else
					{
						pixelmon.setLocationAndAngles(posX, posY, posZ, rotationYaw, 0.0F);
					}
					pixelmon.setMotion(0, 0, 0);
					pixelmon.releaseFromPokeball();
					if (movingobjectposition.entityHit != null && (movingobjectposition.entityHit instanceof IHaveHelper)
							&& !PixelmonStorage.PokeballManager.getPlayerStorage(((EntityPlayerMP) thrower)).isIn(((IHaveHelper) movingobjectposition.entityHit).getHelper()))
						pixelmon.StartBattle(((IHaveHelper) movingobjectposition.entityHit).getHelper());
					if (movingobjectposition.entityHit != null && movingobjectposition.entityHit instanceof EntityTrainer)
						pixelmon.StartBattle((EntityTrainer) movingobjectposition.entityHit, (EntityPlayer) thrower);
					else
						pixelmon.clearAttackTarget();
					if (thrower instanceof EntityPlayer) {

					}
					// spawnCaptureParticles();
				}
				setDead();
			}
		} else {
			if (movingobjectposition != null) {
				ItemPokeBall.ballTimer = 0;
				if (movingobjectposition.entityHit != null && (movingobjectposition.entityHit instanceof IHaveHelper)) {
					IHaveHelper entitypixelmon = (IHaveHelper) movingobjectposition.entityHit;
					p = entitypixelmon.getHelper();
					if (p.getOwner() == (EntityPlayer) thrower) {
						ChatHandler.sendChat((EntityPlayer) thrower, "You can't catch other people's Pokemon!");
						// spawnFailParticles();
						return;
					}
					doCaptureCalc(p);
					isWaiting = true;
					motionX = motionZ = 0;
					motionY = -0.1;
				}
				else {
					Material mat = worldObj.getBlockMaterial(movingobjectposition.blockX, movingobjectposition.blockY, movingobjectposition.blockZ);
					if (!isWaiting && mat != null && mat.isSolid()) {
						if(dropItem)
						{
							entityDropItem(new ItemStack(getType().getItem()), 0.0F);
						}
						setDead();
					}
				}
			}
		}
	}

	int numRocks = 0;
	boolean isUnloaded = false;

	Vec3 initPos;
	Vec3 diff;
	float initialScale;

	@Override
	public void onEntityUpdate() {
		if (worldObj.isRemote) {
			motionX = motionY = motionZ = 0;
		}
		if (isWaiting) {
			if (waitTime == 0 && !isUnloaded) {
				initialScale = p.scale;
				initPos = p.getPosition();
				Vec3 current = Vec3.createVectorHelper(posX, posY, posZ);
				current.xCoord -= initPos.xCoord;
				current.yCoord -= initPos.yCoord;
				current.zCoord -= initPos.zCoord;
				diff = current;
				p.scale = initialScale / 1.1f;
			}
			if (waitTime == 1 && !isUnloaded) {
				p.scale = initialScale / 1.3f;
				moveCloser();
			}
			if (waitTime == 2 && !isUnloaded) {
				p.scale = initialScale / 1.7f;
				moveCloser();
			}
			if (waitTime == 3 && !isUnloaded) {
				p.scale = initialScale / 2.2f;
				moveCloser();
			}
			if (waitTime == 4 && !isUnloaded) {
				p.scale = initialScale / 3;
				moveCloser();
			}
			if (waitTime == 4 && !isUnloaded) {
				p.unloadEntity();
				isUnloaded = true;
				waitTime = 0;
			}
			if (!thrower.worldObj.isAirBlock((int) this.posX, (int) Math.ceil(this.posY) - 2, (int) this.posZ) && this.posY % 1 <= this.height) {
				this.motionY = 0;
				this.motionX = 0;
				this.motionZ = 0;
			}

			waitTime++;
		}
	}

	int i = 0;

	private void moveCloser() {
		p.setPosition(initPos.addVector(diff.xCoord * i / 4, diff.yCoord * i / 4, diff.zCoord * i / 4));
		i++;
	}

	int initialDelay = 15;
	int wobbleTime = 5;
	public boolean flashRed = false;
	int flashTime = 10;
	int flashCounter = 0;

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (!onGround) {
			rotationYaw += 10;
		}
		rotationPitch = 0;
		if (isWaiting) {
			rotationYaw = endRotationYaw;
			flashCounter++;
			if (flashCounter < 15)
				flashRed = true;
			else
				flashRed = false;
			if (flashCounter == 30)
				flashCounter = -1;
		}
		if (isCaptured) {
			if (waitTime > 20) {
				p.setTamed(true);
				p.setOwner((EntityPlayer) thrower);
				p.caughtBall = getType();
				p.clearAttackTarget();
				PixelmonStorage.PokeballManager.getPlayerStorage((EntityPlayerMP) thrower).addToParty(p);
				p.catchInPokeball();
				isWaiting = false;
				setDead();
			}
		} else {
			if (waitTime >= initialDelay && waitTime < initialDelay + wobbleTime) {
				p.scale = initialScale;
				if (numShakes == 0)
					catchPokemon();
				this.rotationPitch = ((float) (waitTime - initialDelay)) / wobbleTime * (float) 35;
			} else if (waitTime >= initialDelay + wobbleTime && waitTime < initialDelay + 3 * wobbleTime) {
				this.rotationPitch = -1 * ((float) (waitTime - (initialDelay + wobbleTime))) / wobbleTime * (float) 35 + 35;
			} else if (waitTime >= initialDelay + 3 * wobbleTime && waitTime < initialDelay + 4 * wobbleTime) {
				this.rotationPitch = ((float) (waitTime - (initialDelay + 3 * wobbleTime))) / wobbleTime * (float) 35 - 35;
			} else if (waitTime == initialDelay + 4 * wobbleTime + initialDelay) {
				waitTime = 0;
				shakeCount++;
				if (shakeCount == numShakes - 1 || numShakes == 1) {
					catchPokemon();
				}
			}
		}
	}

	int numShakes = 0;
	int shakeCount = 0;

	private void doCatchCheck() {

	}

	private void catchPokemon() {
		if (canCatch) {
			ChatHandler.sendChat((EntityPlayer) thrower, "You captured " + p.getName());

			spawnCaptureParticles();
			isCaptured = true;
			waitTime = 0;
		} else {
			spawnFailParticles();
			waitTime = 0;
			isWaiting = false;
			p.getEntity().setPosition(posX, posY, posZ);
			worldObj.spawnEntityInWorld(p.getEntity());
			p.getEntity().setPosition(posX, posY, posZ);
			p.setIsDead(false);
			setDead();
		}
	}

	private void spawnCaptureParticles() {
//		for (int i = RandomHelper.getRandomNumberBetween(5, 7); i > 0; i--) {
//			EntityCrit2FX entitycrit2fx = new EntityCrit2FX(worldObj, this, "crit");
//			ModLoader.getMinecraftInstance().effectRenderer.addEffect(entitycrit2fx);
//		}
	}

	private void spawnFailParticles() {

//		for (int i = 0; i < 30; i++) {
//			EntityReddustFX entityred = new EntityReddustFX(worldObj, posX, posY, posZ, 1, 0, 0);
//			entityred.setVelocity(worldObj.rand.nextFloat() / 5, worldObj.rand.nextFloat() / 5, worldObj.rand.nextFloat() / 5);
//			ModLoader.getMinecraftInstance().effectRenderer.addEffect(entityred);
//		}
	}

	private int b;
	public boolean isCaptured = false;

	protected void doCaptureCalc(PixelmonEntityHelper entitypixelmon) {
		int pokemonRate = entitypixelmon.stats.BaseStats.CatchRate;
		int hpMax = entitypixelmon.getMaxHealth();
		int hpCurrent = entitypixelmon.getHealth();
		int bonusStatus = 1;
		double a, b, p;
		a = (((3 * hpMax - 2 * hpCurrent) * pokemonRate * getType().getBallBonus()) / (3 * hpMax)) * bonusStatus;
		b = (Math.pow(2, 16) - 1) * Math.sqrt(Math.sqrt((a / (Math.pow(2, 8) - 1))));
		p = Math.pow(((b + 1) / Math.pow(2, 16)), 4);
		p = (p * 10000) / 100;
		b = (int) Math.floor(65536 / Math.pow((255 / p), 1f / 4f));
		int passedShakes = 0;
		for (int i = 0; i < 4; i++) {
			int roll = new Random().nextInt(65536);
			if (roll <= b) {
				passedShakes++;
			}
		}
		if (passedShakes == 4) {
			canCatch = true;
		} else {
			canCatch = false;
		}
		numShakes = passedShakes;
	}

	public EnumPokeballs getType() {
		return EnumPokeballs.getFromIndex(dataWatcher.getWatchableObjectInt(10));
	}
}
