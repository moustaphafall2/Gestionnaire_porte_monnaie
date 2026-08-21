package modele.IService;

import java.util.List;
import java.util.Set;

import modele.enumeration.Categorie;
import modele.enumeration.TypeTransaction;

public interface IServiceCategorie {
    
    public Set<Categorie> getCategoriesActives();
    public void activerCategorie(Categorie categorie);
    public void desactiverCategorie(Categorie categorie);
    public boolean aCategorieActiveDeType(TypeTransaction type);
    public List<Categorie> getCategoriesDisponibles();
    public List<Categorie> getCategoriesActivesDeType(TypeTransaction type);
    
}
