package application.service.interfaces;

import java.math.BigDecimal;

public interface IServiceSolde {

    public BigDecimal getSoldeDisponible();
    public BigDecimal getTotalEpargne();
    public BigDecimal soldeApresDepense(BigDecimal montant);
    public boolean depenseRendraSoldeNegatif(BigDecimal montant);
}
