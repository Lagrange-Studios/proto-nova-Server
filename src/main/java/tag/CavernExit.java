package tag;

import protonova.protobuf.EntityProto.Entity;

public class CavernExit extends TagClass {

  public String getTag() {
    return "cavernExit";
  }
  
  /*
   * This class is used to have an entrance from a surface plane into a underground plane on a celestial object
   * Data:
   * 	entranceId: entity id of the entrance from this exit
   */

  public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {

	  Entity entrance = tagHandler.getEntityManager().getEntity(thisEntity.getCustomDataMap().get("entranceId").getIntValue());
	  
	  interactingEntity = interactingEntity.toBuilder()
			  .setMap(entrance.getMap())
			  .setPosition(entrance.getPosition())
			  .build();
	  
	  return interactingEntity;
  }
}
