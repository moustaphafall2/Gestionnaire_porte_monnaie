package modele.IService;

public interface IServicePortefeuille {
   
    public double getSoldeDisponible();
    public double getTotalEpargne();
    public double soldeApresDepense(double montant);
    public void sauvegarder();
}