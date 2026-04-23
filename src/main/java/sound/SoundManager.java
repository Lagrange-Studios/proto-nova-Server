package sound;

import java.util.ArrayList;
import java.util.HashMap;

import entity.ChunkManager;
import file.ServerLoader;
import main.Console;
import main.Server;
import protonova.protobuf.AudioProto.Audio;
import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import socket.Player;
import util.Id;

public class SoundManager {

	private ArrayList<Audio> SoundQueue;
	private ArrayList<Audio> Sounds;
	private HashMap<Long, Long> soundCreationTime; // soundID -> creation time in ticks
	private ServerLoader serverLoader;
	private ChunkManager chunkManager;
	private Console console;
	private Server server;
	private long SoundID = 0;
	private final int SOUND_LIFETIME_TICKS = 120; // ~2 seconds at 60 TPS
	
	public SoundManager(ServerLoader serverLoader,Console console, Server server) {
		this.serverLoader = serverLoader;
		this.server = server;
		SoundQueue = new ArrayList<Audio>();
		Sounds = new ArrayList<Audio>();
		soundCreationTime = new HashMap<>();
		this.console = console;
	}
	
	public void addSoundToQueue(Audio sound) {
		sound = sound.toBuilder().setAudioID(SoundID).build();
		SoundID++;
		SoundQueue.add(sound);
	}
	
	public void processSoundMessagesToSend() {
		removeAllSoundsFromChuncks();
		for (Audio message : SoundQueue) {
			makeNewSound(message);
		}
		SoundQueue.clear();
		
		// Clean up old sound effects to prevent memory leak
		cleanupOldSounds();
	}
	
	private Audio makeNewSound(Audio sound) {
		
		Sounds.add(sound);
		soundCreationTime.put(sound.getAudioID(), server.globalTicks);
		
		if (chunkManager != null) {
			chunkManager.addSound(sound);
		}
		else {
			console.print("WARNING: created Sound without adding it to the chunk manager");
		}
		
		return sound;
		
	}
	
	/**
	 * Removes sound effects older than SOUND_LIFETIME_TICKS
	 */
	private void cleanupOldSounds() {
		long currentTick = server.globalTicks;
		ArrayList<Long> keysToRemove = new ArrayList<>();
		
		for (Long soundID : soundCreationTime.keySet()) {
			if (currentTick - soundCreationTime.get(soundID) > SOUND_LIFETIME_TICKS) {
				keysToRemove.add(soundID);
			}
		}
		
		for (Long soundID : keysToRemove) {
			soundCreationTime.remove(soundID);
			// Remove from sounds list
			Sounds.removeIf(sound -> sound.getAudioID() == soundID);
		}
	}
	
	public void removeAllSoundsFromChuncks() {
		chunkManager.removeAllSounds();
	}
	
	public ArrayList<Audio> getAllSounds() {
		return Sounds;
	}
	
	public void setChunkManager(ChunkManager chunkManager) {
		this.chunkManager = chunkManager;
	}
}
