package org.jmouse.petgallery.pet;

/** A pet card shown in the gallery. */
public class Pet {
    private final long id; private final String name; private final String type; private final String breed;
    private final int age; private final String location; private final String description; private final String emoji; private final String accent;
    public Pet(long id, String name, String type, String breed, int age, String location, String description, String emoji, String accent) {
        this.id=id; this.name=name; this.type=type; this.breed=breed; this.age=age; this.location=location; this.description=description; this.emoji=emoji; this.accent=accent;
    }
    public long getId() { return id; } public String getName() { return name; } public String getType() { return type; }
    public String getBreed() { return breed; } public int getAge() { return age; } public String getLocation() { return location; }
    public String getDescription() { return description; } public String getEmoji() { return emoji; } public String getAccent() { return accent; }
}