package tag;

import protonova.protobuf.CelestialObjectProto.CelestialObject;
import protonova.protobuf.EntityProto.Entity;
import util.DataUtil;

public class CavernEntrance extends TagClass {

  public String getTag() {
    return "cavernEntrance";
  }

  /*
   * This class is used to have an entrance from a surface plane into a underground plane on a celestial object
   * Data:
   * 	exitId: entity id of the exit from this entrance
   */

  public boolean hasSecondTick() {
    return true;
  }

  public void secondTick(TagHandler tagHandler, Entity entity) {
    // if it has not been setup then set it up
    if (!entity.containsCustomData("exitId")) {
      CelestialObject object =
          tagHandler.getCelestialObjectManager().getCelestialObjectFromEntity(entity);

      int undergroundPlaneId = object.getUndergroundPlaneId();

      Entity newExit =
          tagHandler
              .getAssetManager()
              .getEntity("cavern exit", undergroundPlaneId, entity.getPosition());
      newExit =
          newExit.toBuilder().putCustomData("entranceId", DataUtil.newInt(entity.getId())).build();

      tagHandler.updateEntity(newExit);

      entity = entity.toBuilder().putCustomData("exitId", DataUtil.newInt(newExit.getId())).build();

      tagHandler.updateEntity(entity);
    }
  }

  public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {

    Entity exit =
        tagHandler
            .getEntityManager()
            .getEntity(thisEntity.getCustomDataMap().get("exitId").getIntValue());

    interactingEntity =
        interactingEntity.toBuilder().setMap(exit.getMap()).setPosition(exit.getPosition()).build();

    return interactingEntity;
  }
}
