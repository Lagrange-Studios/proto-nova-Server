package tag;

import protonova.protobuf.EntityProto.Entity;

public class CopyClass extends TagClass {

  public String getTag() {
    return "null";
  }

  public boolean hasTick() {
    return false;
  }
  
  public void tick(TagHandler tagHandler, Entity entity) {}

  public boolean hasSecondTick() {
    return false;
  }
  
  public void secondTick(TagHandler tagHandler, Entity entity) {}

  public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {

    return interactingEntity;
  }
}
