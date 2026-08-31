package application.service.interfaces;

public interface IServiceSolde {

    public double getSoldeDisponible();
    public double getTotalEpargne();
    public double soldeApresDepense(double montant);
    public boolean depenseRendraSoldeNegatif(double montant);
}
