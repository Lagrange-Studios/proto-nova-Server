package tag;

import protonova.protobuf.EntityProto.Entity;

public class BerryBush extends TagClass {

	public static final String tag = "null";
	
	public static void tick(TagHandler tagHandler, Entity entity) {
		System.out.println("Tick bush");
	}
}
