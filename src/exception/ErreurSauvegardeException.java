package exception;

/*
    * Levée quand l'écriture en base échoue (connexion perdue, contrainte violée). Persister
    * précède toujours la mutation en mémoire : un échec signifie que l'opération n'a eu lieu
    * nulle part.
*/
public class ErreurSauvegardeException extends RuntimeException {
    public ErreurSauvegardeException(String message, Throwable cause) {
        super(message, cause);
    }
}
