package sound;

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

	private HashMap<Audio, Long> sounds;
	private ServerLoader serverLoader;
	private ChunkManager chunkManager;
	private Console console;
	private Server server;

	public SoundManager(ServerLoader serverLoader,Console console, Server server) {
		this.serverLoader = serverLoader;
		this.server = server;
		sounds = new HashMap();
		this.console = console;
	}
	
	public Audio makeNewSound(Audio audio) {
		
		sounds.put(audio, server.globalTicks);
		
		if (chunkManager != null) {
			chunkManager.addSound(audio);
		}
		else {
			console.print("WARNING: created entity without adding it to the chunk manager");
		}
		
		return audio;
		
	}
	
	public Long getSoundTime(Audio audio) {
		return (Long) sounds.get(audio);
	}
	
	public HashMap<Audio, Long> getAllSounds() {
		return sounds;
	}
	
	public void updateSound(Audio audio) {
		if (sounds.get(audio) + 60 < server.globalTicks) {
			sounds.remove(audio);
		}
	}

	public void setChunkManager(ChunkManager chunkManager) {
		this.chunkManager = chunkManager;
	}
}
