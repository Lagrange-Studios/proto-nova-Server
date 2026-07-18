package sound;

import protonova.protobuf.AudioProto.Audio;
import protonova.protobuf.AudioProto.AudioType;
import protonova.protobuf.VectorProto.Vector;

public class AudioBuilder {
    
    public static Audio createSoundEffect(String name, Vector position, int map, float volume) {
        return Audio.newBuilder()
                .setName(name)
                .setAudioType(AudioType.SOUND_EFFECT)
                .setPosition(position)
                .setMap(map)
                .setVolume((int) (volume * 100))
                .build();
    }
    
    public static Audio createSoundEffectAtEntity(String name, int entityID, int map, float volume) {
        return Audio.newBuilder()
                .setName(name)
                .setAudioType(AudioType.SOUND_EFFECT)
                .setEntityID(entityID)
                .setMap(map)
                .setVolume((int) (volume * 100))
                .build();
    }
    
    public static Audio createAmbientSound(String name, Vector position, int map, float volume) {
        return Audio.newBuilder()
                .setName(name)
                .setAudioType(AudioType.AMBIENT)
                .setPosition(position)
                .setMap(map)
                .setVolume((int) (volume * 100))
                .build();
    }
    
    public static Audio createMusic(String name, int map, float volume) {
        return Audio.newBuilder()
                .setName(name)
                .setAudioType(AudioType.MUSIC)
                .setMap(map)
                .setVolume((int) (volume * 100))
                .build();
    }
    
    public static Audio createVoice(String name, int entityID, int map, float volume) {
        return Audio.newBuilder()
                .setName(name)
                .setAudioType(AudioType.VOICE)
                .setEntityID(entityID)
                .setMap(map)
                .setVolume((int) (volume * 100))
                .build();
    }
    
    public static float getVolumeAsFloat(Audio audio) {
        return Math.min(1.0f, audio.getVolume() / 100.0f);
    }
}
