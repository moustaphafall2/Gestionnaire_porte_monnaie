package application.service.interfaces;

import java.util.List;
import java.util.Set;

import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;

public interface IServiceCategorie {
    
    public Set<Categorie> getCategoriesActives();
    public void activerCategorie(Categorie categorie);
    public void desactiverCategorie(Categorie categorie);
    public boolean aCategorieActiveDeType(TypeTransaction type);
    public List<Categorie> getCategoriesDisponibles();
    public List<Categorie> getCategoriesActivesDeType(TypeTransaction type);
    
}
