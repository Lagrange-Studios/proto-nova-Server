package sound;

import entity.ChunkManager;
import entity.EntityManager;
import file.ServerLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import main.Console;
import main.Server;
import protonova.protobuf.AudioProto.Audio;
import protonova.protobuf.EntityProto.Entity;
import socket.Player;
import util.AudioBuilder;

public class SoundManager {

  private final ConcurrentLinkedQueue<Audio> soundQueue = new ConcurrentLinkedQueue<>();
  private final ArrayList<Audio> sounds = new ArrayList<>();
  private final AtomicLong nextSoundId = new AtomicLong(1);
  private final Map<Integer, Integer> movingPlayers = new HashMap<>();
  private final Console console;
  private ChunkManager chunkManager;

  public SoundManager(ServerLoader serverLoader, Console console, Server server) {
    this.console = console;
  }

  public void addSoundToQueue(Audio sound) {
    if (!isValid(sound)) return;
    soundQueue.offer(
        sound.toBuilder().setVolume(Math.max(0, Math.min(100, sound.getVolume()))).build());
  }

  public void emit(Audio sound) {
    addSoundToQueue(sound);
  }

  public void processPlayerMovement(Iterable<Player> players, EntityManager entityManager) {
    Set<Integer> connectedPlayers = new HashSet<>();
    for (Player player : players) {
      Entity entity = entityManager.getEntity(player);
      if (entity == null) continue;
      connectedPlayers.add(entity.getId());
      boolean moving =
          Math.abs(entity.getVelocity().getX()) > 0.01f
              || Math.abs(entity.getVelocity().getY()) > 0.01f;
      Integer previousMap = movingPlayers.get(entity.getId());
      if (moving && previousMap == null) {
        emit(
            AudioBuilder.createLoopingSoundEffectAtEntity(
                    "walking", entity.getId(), entity.getMap(), 0.7f)
                .toBuilder()
                .setOriginEntityID(entity.getId())
                .build());
        movingPlayers.put(entity.getId(), entity.getMap());
      } else if (moving && previousMap != entity.getMap()) {
        emit(
            AudioBuilder.stopLoop("walking", entity.getId(), previousMap).toBuilder()
                .setOriginEntityID(entity.getId())
                .build());
        emit(
            AudioBuilder.createLoopingSoundEffectAtEntity(
                    "walking", entity.getId(), entity.getMap(), 0.7f)
                .toBuilder()
                .setOriginEntityID(entity.getId())
                .build());
        movingPlayers.put(entity.getId(), entity.getMap());
      } else if (!moving && previousMap != null) {
        movingPlayers.remove(entity.getId());
        emit(
            AudioBuilder.stopLoop("walking", entity.getId(), previousMap).toBuilder()
                .setOriginEntityID(entity.getId())
                .build());
      }
    }
    movingPlayers.keySet().retainAll(connectedPlayers);
  }

  public void processSoundMessagesToSend() {
    if (chunkManager == null) return;
    chunkManager.removeAllSounds();
    sounds.clear();
    Audio queued;
    while ((queued = soundQueue.poll()) != null) {
      Audio sound = queued.toBuilder().setAudioID(nextSoundId.getAndIncrement()).build();
      if (!sound.hasPosition() && !sound.hasEntityID()) {
        sounds.add(sound);
      } else if (chunkManager.addSound(sound)) sounds.add(sound);
      else console.print("WARNING: rejected sound without a valid position: " + sound.getName());
    }
  }

  public void removeAllSoundsFromChuncks() {
    if (chunkManager != null) chunkManager.removeAllSounds();
  }

  public ArrayList<Audio> getAllSounds() {
    return sounds;
  }

  public void setChunkManager(ChunkManager chunkManager) {
    this.chunkManager = chunkManager;
  }

  private static boolean isValid(Audio sound) {
    if (sound == null) return false;
    String name = sound.getName().trim();
    return !name.isEmpty()
        && name.length() <= 64
        && !name.contains("/")
        && !name.contains("\\")
        && !name.contains("..");
  }
}
