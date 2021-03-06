package edu.semweb.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Bean class for representing co-occurring entities in a sentence, along with their types.
 * @author Prashanth Govindaraj
 *
 */
public class RelationsInSentence {

	/**
	 * nerTaggedEntityMap - HashMap whose keys are entity types and values are a list of entity names corresponding to each type.
	 * 						Type could only be LOCATION, ORGANIZATION, DATE, MONEY, PERSON, PERCENT or TIME
	 * sentence -			The original sentence
	 * entityCount -		Number of distinct entities in the sentence
	 */
	private final HashMap<String, List<String>> nerTaggedEntityMap;
	private final String sentence;
	private int entityCount;

	public RelationsInSentence(final String sourceSentence) {
		nerTaggedEntityMap = new HashMap<>(7);
		sentence = sourceSentence;
		entityCount = 0;
	}

	/**
	 * Adds an entity to nerTaggedEntityMap
	 * @param entityName	The entity name, e.g., Bill Gates, Microsoft, Washington, etc.
	 * @param entityType	Type of the entity
	 * @return	true if the entity was successfully inserted, false otherwise (if a similar entity already exists. REFER: containsSimilarItem() defined later)
	 */
	public boolean addEntity(final String entityName, final String entityType) {
		if(!entityType.matches("LOCATION|ORGANIZATION|DATE|MONEY|PERSON|PERCENT|TIME")) {
			return false;
		}
		if(nerTaggedEntityMap.containsKey(entityType)) {
			final List<String> entityList = nerTaggedEntityMap.get(entityType);
			if(!containsSimilarEntity(entityList, entityName)) {
				entityList.add(entityName);
				nerTaggedEntityMap.put(entityType, entityList);
				entityCount++;
				return true;
			}
			return false;
		}
		else {
			final List<String> entityList = new ArrayList<>();
			entityList.add(entityName);
			nerTaggedEntityMap.put(entityType,entityList);
			entityCount++;
			return true;
		}
	}

	/**
	 * If we encounter an entity name that is partially equivalent to an entity that was already encountered in the same sentence, it is most likely to be trivial.
	 * This method checks that case. For example, encountering "Gates" after "Bill Gates" was already encountered in the same sentence.
	 * @param entityList	Should be a List<String> from the value set of nerTaggedEntityMap
	 * @param entityName	Name of the newly encountered entity
	 * @return	true if a similar entity was encountered already, false otherwise
	 */
	private boolean containsSimilarEntity(final List<String> entityList, final String entityName) {
		for(final String entity : entityList) {
			if(entity.toLowerCase().contains(entityName.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the map containing all entities encountered in this sentence
	 * @return	the map containing all entities encountered in this sentence
	 */
	public HashMap<String, List<String>> getNerTaggedEntityMap() {
		return nerTaggedEntityMap;
	}

	/**
	 * Returns the source sentence
	 * @return	the source sentence
	 */
	public String getSentence() {
		return sentence;
	}

	/**
	 * We are interested only in relationships involving people or organizations.
	 * If there is no PERSON or ORGANIZATION entity in the sentence, or if there aren't at least 2 entities in co-occurrence,
	 * the relationship between the entities in this sentence are trivial.
	 * @return	true if the co-occurrences are trivial, false otherwise
	 */
	public boolean isEmpty() {
		if (nerTaggedEntityMap.get("PERSON") == null && nerTaggedEntityMap.get("ORGANIZATION") == null) {
			return true;
		} else {
			return entityCount <= 1;
		}
	}

	public List<Triple> getAllTriple(final String sourceUrl) {
		final String[] types = {"PERSON", "ORGANIZATION"};
		final List<Triple> triples = new ArrayList<>();
		for (final String type : types) {
			if(nerTaggedEntityMap.containsKey(type)) {
				triples.addAll(getTriplesForType(type, sourceUrl));
			}
		}
		return triples;
	}

	private List<Triple> getTriplesForType(final String subjectType, final String sourceUrl) {
		final List<Triple> triples = new ArrayList<>();
		final List<String> entities = nerTaggedEntityMap.get(subjectType);
		for(int i = 0; i < entities.size(); i++) {
			final String subject = entities.get(i);
			for(final Entry<String, List<String>> entry : nerTaggedEntityMap.entrySet()) {
				final String objectType = entry.getKey();
				final List<String> objectsList = entry.getValue();
				for(int j = 0; j < objectsList.size(); j++) {
					if(objectType.equals(subjectType) && i==j) {
						continue;
					}
					final String object = objectsList.get(j);
					triples.add(new Triple(subject, getOwlClassForType(subjectType), getOwlPredicateForType(objectType), object, getOwlClassForType(objectType), sentence, sourceUrl));
				}
			}
		}
		return triples;
	}

	private String getOwlClassForType(final String type) {
		switch(type) {
		case "PERSON":
			return "Person";
		case "ORGANIZATION":
			return "Organization";
		case "LOCATION":
			return "Location";
		case "DATE":
			return "Date";
		case "MONEY":
			return "Money";
		default:
			return null;
		}
	}

	private String getOwlPredicateForType(final String type) {
		switch(type) {
		case "PERSON":
			return "hasPerson";
		case "ORGANIZATION":
			return "hasOrganization";
		case "LOCATION":
			return "hasLocation";
		case "DATE":
			return "hasDate";
		case "MONEY":
			return "hasMoney";
		default:
			return null;
		}
	}

	/**
	 * Returns a pretty string representation of this RelationsInSentence object
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Sentence: \""+sentence+"\"");
		for(final Entry<String, List<String>> entry : nerTaggedEntityMap.entrySet()) {
			sb.append("\n    "+entry.getKey()+"(S): ");
			for(final String entity : entry.getValue()) {
				sb.append(entity+", ");
			}
		}
		return sb.toString();
	}

}
