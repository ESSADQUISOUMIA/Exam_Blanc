package examen;

public aspect Logging {
    pointcut applicationMethods():
        execution(* examen..*(..)) && !within(Logging) && !within(Security);

    before(): applicationMethods() {
        System.err.println("ENTREE " + thisJoinPointStaticPart.getSignature());
    }
    after() returning: applicationMethods() {
        System.err.println("SUCCES " + thisJoinPointStaticPart.getSignature());
    }
    after() throwing(Throwable error): applicationMethods() {
        System.err.println("ECHEC " + thisJoinPointStaticPart.getSignature()
            + " " + error.getClass().getSimpleName());
    }
}
