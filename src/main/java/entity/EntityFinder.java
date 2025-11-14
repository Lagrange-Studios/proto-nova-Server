package entity;

import java.util.HashMap;

import protonova.protobuf.EntityProto.Entity;

public class EntityFinder {
	private HashMap<Integer, Entity> entities;
	
	public EntityFinder(HashMap<Integer, Entity> allEntities, ChunckManager chunckManager) {
		entities = allEntities;
	}

}
