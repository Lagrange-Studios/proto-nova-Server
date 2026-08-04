package sound;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import entity.ChunkManager;
import file.ServerLoader;
import main.Console;
import main.Server;
import protonova.protobuf.AudioProto.Audio;
import protonova.protobuf.AudioProto.AudioType;
import protonova.protobuf.EntityProto.Entity;

public class SoundManager {

	private final ConcurrentLinkedQueue<Audio> soundQueue = new ConcurrentLinkedQueue<>();
	private final ArrayList<Audio> sounds = new ArrayList<>();
	private final AtomicLong nextSoundId = new AtomicLong(1);
	private final Console console;
	private ChunkManager chunkManager;

	public SoundManager(ServerLoader serverLoader, Console console, Server server) {
		this.console = console;
	}

	public void addSoundToQueue(Audio sound) {
		if (sound == null || sound.getName().trim().isEmpty()) return;
		soundQueue.offer(sound.toBuilder()
				.setVolume(Math.max(0, Math.min(100, sound.getVolume())))
				.build());
	}

	public void emit(Audio sound) {
		addSoundToQueue(sound);
	}

	public void processPlayerMovement(Map<Integer, Entity> players) {}

	public void processSoundMessagesToSend() {
		if (chunkManager == null) return;
		chunkManager.removeAllSounds();
		sounds.clear();
		Audio queued;
		while ((queued = soundQueue.poll()) != null) {
			Audio sound = queued.toBuilder().setAudioID(nextSoundId.getAndIncrement()).build();
			if (!sound.hasPosition() && !sound.hasEntityID()
					&& (sound.getAudioType() == AudioType.MUSIC || sound.getStop())) {
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
}
