package entity;

import java.util.HashMap;

import protonova.protobuf.EntityProto.Entity;

public class EntityManager {

	private HashMap<Integer, Entity> entities;

	public EntityManager(HashMap<Integer, Entity> entities) {
		this.entities = entities;
	}
	
}
