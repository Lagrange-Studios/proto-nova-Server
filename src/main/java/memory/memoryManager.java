package memory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class memoryManager {

  private Set<String> seenEntities;

  private Set<String> knownEntities;

  private ConcurrentHashMap<String, Set<String>> recipeSlotRevelations;

  private Set<String> knownIngredients;

  public memoryManager() {
    this.seenEntities = ConcurrentHashMap.newKeySet();
    this.knownEntities = ConcurrentHashMap.newKeySet();
    this.recipeSlotRevelations = new ConcurrentHashMap<>();
    this.knownIngredients = ConcurrentHashMap.newKeySet();
  }

  public void seenEntity(String entityName) {
    if (seenEntities.add(entityName)) {
      revealRecipeSlotsForEntity(entityName);
    }
  }

  public boolean knowsEntity(String entityName) {
    return knownEntities.contains(entityName);
  }

  public void knowEntity(String entityName) {
    knownEntities.add(entityName);
  }

  private void revealRecipeSlotsForEntity(String entityName) {
    Set<String> revealedSlots = recipeSlotRevelations.getOrDefault(entityName, new HashSet<>());

    knownIngredients.addAll(revealedSlots);

    knownEntities.add(entityName);
  }

  public boolean isIngredientKnown(String ingredientName) {
    return knownIngredients.contains(ingredientName);
  }

  public Set<String> getKnownIngredients() {
    return new HashSet<>(knownIngredients);
  }

  public void addRecipeSlotRevelation(String entityName, String slotName) {
    recipeSlotRevelations.computeIfAbsent(entityName, k -> new HashSet<>()).add(slotName);
  }

  public void resetAllKnowledge() {
    seenEntities.clear();
    knownEntities.clear();
    knownIngredients.clear();
  }

  public int getKnownIngredientCount() {
    return knownIngredients.size();
  }

  public boolean hasAnyDiscoveries() {
    return !seenEntities.isEmpty();
  }

  public Set<String> getDiscoveredEntities() {
    return new HashSet<>(seenEntities);
  }
}
