package exception;

/*
    * Levée quand le chargement échoue pour une raison qui ne peut pas s'absorber en repartant
    * d'un portefeuille vide : connexion refusée, db.properties absent ou illisible, erreur SQL.
    * Une base neuve sans donnée ne lève jamais cette exception.
*/
public class ErreurChargementException extends RuntimeException {
    public ErreurChargementException(String message, Throwable cause) {
        super(message, cause);
    }
}
