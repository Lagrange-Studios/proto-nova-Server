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
	private ServerLoader serverLoader;
	private ChunkManager chunkManager;
	private Console console;
	private Server server;
	private long SoundID = 0;
	
	public SoundManager(ServerLoader serverLoader,Console console, Server server) {
		this.serverLoader = serverLoader;
		this.server = server;
		SoundQueue = new ArrayList<Audio>();
		Sounds = new ArrayList<Audio>();
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
		
	}
	
	private Audio makeNewSound(Audio sound) {
		
		Sounds.add(sound);		
		
		if (chunkManager != null) {
			chunkManager.addSound(sound);
		}
		else {
			console.print("WARNING: created Sound without adding it to the chunk manager");
		}
		
		return sound;
		
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
