package examen;

public aspect Security {
    declare precedence: Logging, Security, *;

    before(): execution(* examen.Exam.NeuralNetwork.fit(..))
           || execution(* examen.Exam.NeuralNetwork.train(..)) {
        if (Exam.ROLE.get() != Exam.Role.TRAINER) {
            throw new SecurityException("Role TRAINER requis");
        }
    }
    before(): execution(* examen.Exam.NeuralNetwork.predict(..)) {
        if (Exam.ROLE.get() == null) {
            throw new SecurityException("Authentification requise");
        }
    }
}
