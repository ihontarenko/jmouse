package org.jmouse.petgallery.pet;

import org.jmouse.beans.annotation.Bean;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** In-memory catalogue; replace this bean with a JDBC implementation later. */
@Bean
public class PetCatalog {
    private final List<Pet> pets = List.of(
            new Pet(1, "Milo", "Dog", "Corgi", 2, "Kyiv", "A cheerful little explorer who adores long walks.", "🐕", "peach"),
            new Pet(2, "Luna", "Cat", "British Shorthair", 3, "Lviv", "Quiet, affectionate and always ready for a sunny nap.", "🐈", "lavender"),
            new Pet(3, "Poppy", "Rabbit", "Mini Lop", 1, "Odesa", "Tiny ears, big curiosity, and a taste for fresh herbs.", "🐇", "mint"),
            new Pet(4, "Archie", "Dog", "Golden Retriever", 4, "Dnipro", "A gentle companion with an endless supply of tail wags.", "🐶", "sky"),
            new Pet(5, "Nori", "Cat", "European Shorthair", 2, "Kyiv", "Clever, playful and surprisingly good at hide-and-seek.", "🐱", "butter"),
            new Pet(6, "Kiwi", "Bird", "Budgerigar", 1, "Vinnytsia", "A bright little singer looking for a sunny windowsill.", "🦜", "coral"));
    public List<Pet> find(String type, String query) {
        String typeFilter=normalize(type), textFilter=normalize(query);
        return pets.stream().filter(p -> typeFilter.isEmpty() || p.getType().equalsIgnoreCase(typeFilter))
                .filter(p -> textFilter.isEmpty() || matches(p, textFilter)).toList();
    }
    public Optional<Pet> findById(long id) { return pets.stream().filter(p -> p.getId()==id).findFirst(); }
    public int size() { return pets.size(); }
    private boolean matches(Pet pet, String query) { return (pet.getName()+" "+pet.getBreed()+" "+pet.getLocation()).toLowerCase(Locale.ROOT).contains(query); }
    private String normalize(String value) { return value==null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}