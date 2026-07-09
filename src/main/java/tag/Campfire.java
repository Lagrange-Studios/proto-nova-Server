package tag;

public class Campfire extends Furnace {

	@Override
	public String getTag() {
		return "campfire";
	}

	@Override
	protected String getFuelType() {
		return "basicFuel";
	}

	@Override
	protected int getFuelTimeAdded() {
		return 20 * 20;
	}

	@Override
	protected int getTicksBetweenFrames()  {
		return 5;
	}

	@Override
	protected String getHexColor()  {
		return "FF0000";
	}

	@Override
	protected float getLightRange() {
		return 7.5f;
	}
	
	/*
	 * Extension of the furnace class 
	 */
}
