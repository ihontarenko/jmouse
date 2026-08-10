package org.jmouse.petgallery.web;

import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.petgallery.pet.Pet;
import org.jmouse.petgallery.pet.PetCatalog;
import org.jmouse.web.annotation.Controller;
import org.jmouse.web.annotation.GetMapping;
import org.jmouse.web.annotation.PathVariable;
import org.jmouse.web.annotation.RequestParameter;
import org.jmouse.web.mvc.Model;
import java.util.List;

/** MVC endpoints for the Pet Gallery UI and JSON catalogue. */
@Controller
public class PetGalleryController {
    private final PetCatalog catalog;
    @BeanConstructor public PetGalleryController(PetCatalog catalog) { this.catalog=catalog; }
    @GetMapping(requestPath = "/")
    public String gallery(@RequestParameter("type") String type, @RequestParameter("q") String query, Model model) {
        model.addAttribute("pets", catalog.find(type, query));
        model.addAttribute("selectedType", type == null ? "" : type);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("totalPets", catalog.size());
        return "view:gallery/index";
    }
    @GetMapping(requestPath = "/pets/{id}")
    public String detail(@PathVariable("id") long id, Model model) {
        Pet pet=catalog.findById(id).orElseThrow(() -> new IllegalArgumentException("Pet not found: "+id));
        model.addAttribute("pet", pet); return "view:gallery/detail";
    }
    @GetMapping(requestPath = "/api/pets", produces = "application/json")
    public List<Pet> pets(@RequestParameter("type") String type, @RequestParameter("q") String query) { return catalog.find(type, query); }
}