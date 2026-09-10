package tag;

import protonova.protobuf.EntityProto.Entity;

public class Shirt extends TagClass {

  public String getTag() {
    return "shirt";
  }

  /*
   * Makes a item equipable based on the other tag
   *
   */

  public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {

    if (!interactingEntity.containsInventorySlots("shirt")) {
      interactingEntity =
          interactingEntity.toBuilder()
              .removeInventorySlots(interactingEntity.getSelectedSlot())
              .putInventorySlots("shirt", thisEntity.getId())
              .build();
    }

    return interactingEntity;
  }
}
