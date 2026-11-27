package examen;

import java.util.*;

public class Exam {
    // Strategy et Adapter
    interface Activation {
        double apply(double x);
        double slope(double x);
    }
    record Identity() implements Activation {
        public double apply(double x) { return x; }
        public double slope(double x) { return 1; }
    }
    record Sigmoid() implements Activation {
        public double apply(double x) { return 1 / (1 + Math.exp(-x)); }
        public double slope(double x) {
            double y = apply(x);
            return y * (1 - y);
        }
    }
    static class LegacyTanh {
        double evaluate(double x) { return Math.tanh(x); }
    }
    record ActivationAdapter(LegacyTanh legacy) implements Activation {
        public double apply(double x) { return legacy.evaluate(x); }
        public double slope(double x) {
            double y = apply(x);
            return 1 - y * y;
        }
    }

    static class Neuron {
        final Activation activation;
        final List<Connection> incoming = new ArrayList<>();
        double bias, sum, output, gradient;

        Neuron(Activation activation) { this.activation = activation; }

        void forward() {
            sum = bias;
            for (Connection c : incoming) {
                sum += c.source.output * c.weight;
            }
            output = activation.apply(sum);
        }
    }
    static class Connection {
        final Neuron source, destination;
        private double weight;

        Connection(Neuron source, Neuron destination, double weight) {
            if (!Double.isFinite(weight) || weight < 0 || weight > 1) {
                throw new IllegalArgumentException("Poids hors de [0, 1]");
            }
            this.source = source;
            this.destination = destination;
            this.weight = weight;
        }

        void update(double change) {
            weight = Math.max(0, Math.min(1, weight + change));
        }
    }

    // Component et Decorator
    interface Layer {
        List<Neuron> neurons();
        default void forward() { neurons().forEach(Neuron::forward); }
    }
    record InputLayer(List<Neuron> neurons) implements Layer {
        public void forward() {} // Les valeurs d'entrée sont déjà chargées.
    }
    record HiddenLayer(List<Neuron> neurons) implements Layer {}
    record OutputLayer(List<Neuron> neurons) implements Layer {}

    abstract static class LayerDecorator implements Layer {
        final Layer inner;
        LayerDecorator(Layer inner) { this.inner = inner; }
        public List<Neuron> neurons() { return inner.neurons(); }
        public void forward() { inner.forward(); }
    }
    static class CountedLayer extends LayerDecorator {
        int passes;
        CountedLayer(Layer inner) { super(inner); }
        public void forward() {
            super.forward();
            passes++;
        }
    }

    // State et Observer
    interface State {
        default void fit(NeuralNetwork n, double[][] x, double[][] y, int epochs) {
            throw new IllegalStateException("Entrainement indisponible");
        }
        default double[] predict(NeuralNetwork n, double[] input) {
            throw new IllegalStateException("Reseau non entraine");
        }
    }
    record Construction() implements State {
        public void fit(NeuralNetwork n, double[][] x, double[][] y, int epochs) {
            n.learn(x, y, epochs);
        }
    }
    record Training() implements State {}
    record Trained() implements State {
        public double[] predict(NeuralNetwork n, double[] input) {
            NeuralNetwork.validate(input, n.input.neurons().size());
            n.forward(input);
            return n.layers.getLast().neurons().stream()
                .mapToDouble(neuron -> neuron.output).toArray();
        }
    }
    interface Observer {
        void changed(State state);
    }
    static class Dashboard implements Observer {
        final List<String> states = new ArrayList<>();
        public void changed(State state) {
            states.add(state.getClass().getSimpleName());
        }
    }

    static class NeuralNetwork {
        private final Layer input;
        private final List<Layer> layers;
        private final List<Observer> observers = new ArrayList<>();
        private State state = new Construction();

        NeuralNetwork(Layer input, List<Layer> layers) {
            this.input = input;
            this.layers = List.copyOf(layers);
        }
        synchronized void subscribe(Observer observer) {
            observers.add(Objects.requireNonNull(observer));
        }
        private void transition(State next) {
            state = next;
            for (Observer observer : List.copyOf(observers)) {
                try {
                    observer.changed(next);
                } catch (RuntimeException error) {
                    System.err.println("Observateur : " + error.getMessage());
                }
            }
        }
        synchronized void fit(double[][] x, double[][] y, int epochs) {
            state.fit(this, x, y, epochs);
        }
        synchronized void train(double[][] x, double[][] y, int epochs) {
            fit(x, y, epochs);
        }
        synchronized double[] predict(double[] input) {
            return state.predict(this, input);
        }
        private static void validate(double[] vector, int size) {
            if (vector == null || vector.length != size ||
                    Arrays.stream(vector).anyMatch(v -> !Double.isFinite(v))) {
                throw new IllegalArgumentException("Vecteur invalide");
            }
        }
        private void forward(double[] x) {
            for (int i = 0; i < x.length; i++) {
                input.neurons().get(i).output = x[i];
            }
            input.forward();
            layers.forEach(Layer::forward);
        }
        private void learn(double[][] x, double[][] y, int epochs) {
            if (x == null || y == null || x.length == 0 ||
                    x.length != y.length || epochs <= 0) {
                throw new IllegalArgumentException("Jeu ou nombre d'epoques invalide");
            }
            int outputs = layers.getLast().neurons().size();
            for (int i = 0; i < x.length; i++) {
                validate(x[i], input.neurons().size());
                validate(y[i], outputs);
            }
            transition(new Training());
            try {
                for (int epoch = 0; epoch < epochs; epoch++) {
                    for (int i = 0; i < x.length; i++) {
                        forward(x[i]);
                        backward(y[i]);
                    }
                }
                transition(new Trained());
            } catch (RuntimeException error) {
                transition(new Construction());
                throw error;
            }
        }

        private void backward(double[] target) {
            for (Layer layer : layers) {
                layer.neurons().forEach(neuron -> neuron.gradient = 0);
            }
            input.neurons().forEach(neuron -> neuron.gradient = 0);

            List<Neuron> output = layers.getLast().neurons();
            for (int i = 0; i < target.length; i++) {
                output.get(i).gradient = output.get(i).output - target[i];
            }
            for (Layer layer : layers.reversed()) {
                for (Neuron neuron : layer.neurons()) {
                    double delta = neuron.gradient * neuron.activation.slope(neuron.sum);
                    neuron.bias -= 0.1 * delta;
                    for (Connection c : neuron.incoming) {
                        c.source.gradient += c.weight * delta;
                        c.update(-0.1 * c.source.output * delta);
                    }
                }
            }
        }
    }

    // Builder : construit les liaisons entre couches adjacentes.
    static class Builder {
        private record Spec(int size, Activation activation, boolean counted) {}
        private final int inputs;
        private boolean countInput, countOutput;
        private final List<Spec> hidden = new ArrayList<>();

        Builder(int inputs) {
            if (inputs < 1) throw new IllegalArgumentException("Entree vide");
            this.inputs = inputs;
        }
        Builder countInput() {
            countInput = true;
            return this;
        }
        Builder countOutput() {
            countOutput = true;
            return this;
        }
        Builder hidden(int size, Activation activation, boolean counted) {
            hidden.add(spec(size, activation, counted));
            return this;
        }
        private Spec spec(int size, Activation activation, boolean counted) {
            if (size < 1) throw new IllegalArgumentException("Couche vide");
            return new Spec(size, Objects.requireNonNull(activation), counted);
        }
        private List<Neuron> neurons(int size, Activation activation) {
            List<Neuron> neurons = new ArrayList<>();
            for (int i = 0; i < size; i++) neurons.add(new Neuron(activation));
            return List.copyOf(neurons);
        }
        NeuralNetwork build(int outputs, Activation activation) {
            List<Spec> specs = new ArrayList<>(hidden);
            specs.add(spec(outputs, activation, countOutput));
            Layer input = new InputLayer(neurons(inputs, new Identity()));
            if (countInput) input = new CountedLayer(input);
            List<Layer> layers = new ArrayList<>();
            Layer previous = input;
            Random random = new Random(42);
            for (int i = 0; i < specs.size(); i++) {
                Spec s = specs.get(i);
                List<Neuron> neurons = neurons(s.size(), s.activation());
                for (Neuron destination : neurons) {
                    for (Neuron source : previous.neurons()) {
                        destination.incoming.add(
                            new Connection(source, destination, random.nextDouble()));
                    }
                }
                Layer layer = i == specs.size() - 1
                    ? new OutputLayer(neurons) : new HiddenLayer(neurons);
                if (s.counted()) layer = new CountedLayer(layer);
                layers.add(layer);
                previous = layer;
            }
            return new NeuralNetwork(input, layers);
        }
    }

    // Identité de test fournie par l'appelant de confiance.
    enum Role { TRAINER, READER }
    static final ThreadLocal<Role> ROLE = new ThreadLocal<>();

    static void require(boolean condition, String test) {
        if (!condition) throw new AssertionError(test);
        System.out.println("OK - " + test);
    }
    static void expect(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            require(expected.isInstance(error), expected.getSimpleName());
            return;
        }
        throw new AssertionError("Exception attendue : " + expected.getSimpleName());
    }
    static void deniedOperations() {
        NeuralNetwork network = new Builder(1).build(1, new Identity());
        ROLE.set(Role.READER);
        try {
            expect(SecurityException.class,
                () -> network.fit(new double[][]{{1}}, new double[][]{{1}}, 1));
            expect(SecurityException.class,
                () -> network.train(new double[][]{{1}}, new double[][]{{1}}, 1));
        } finally {
            ROLE.remove();
        }
        expect(SecurityException.class, () -> network.predict(new double[]{1}));
    }

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--woven")) deniedOperations();
        ROLE.set(Role.TRAINER);
        try {
            NeuralNetwork n = new Builder(3).countInput().countOutput()
                .hidden(5, new Sigmoid(), true)
                .build(2, new Sigmoid());
            Dashboard dashboard = new Dashboard();
            n.subscribe(dashboard);
            n.subscribe(state -> {
                if (state instanceof Training) {
                    expect(IllegalStateException.class,
                        () -> n.predict(new double[3]));
                }
            });
            expect(IllegalStateException.class, () -> n.predict(new double[3]));
            expect(IllegalArgumentException.class,
                () -> n.fit(new double[][]{{1}}, new double[][]{{1, 0}}, 1));
            require(dashboard.states.isEmpty(), "Validation avant transition");
            double[][] x = {{0, 0, 0}, {1, 1, 1}};
            double[][] y = {{0, 0}, {1, 1}};
            n.train(x, y, 1500);
            require(dashboard.states.equals(List.of("Training", "Trained")),
                "Notifications dans l'ordre");
            ROLE.set(Role.READER);
            double[] low = n.predict(x[0]);
            double[] high = n.predict(x[1]);
            require(low.length == 2 && high.length == 2, "Sortie de dimension 2");
            require(low[0] < 0.2 && low[1] < 0.2
                && high[0] > 0.8 && high[1] > 0.8, "Apprentissage");
            require(((CountedLayer) n.layers.getFirst()).passes == 3002,
                "Comptage des propagations");
            require(((CountedLayer) n.input).passes == 3002
                && ((CountedLayer) n.layers.getLast()).passes == 3002,
                "Decoration des couches d'entree et de sortie");
            require(new ActivationAdapter(new LegacyTanh()).apply(0) == 0,
                "Adaptation de la fonction historique");
            require(n.layers.stream().flatMap(l -> l.neurons().stream())
                .flatMap(neuron -> neuron.incoming.stream()).count() == 25,
                "25 connexions pour 3-5-2");
            require(n.layers.stream().flatMap(l -> l.neurons().stream())
                .flatMap(neuron -> neuron.incoming.stream())
                .allMatch(c -> c.weight >= 0 && c.weight <= 1), "Poids bornes");
            expect(IllegalArgumentException.class, () -> n.predict(new double[2]));
            System.out.println(Arrays.toString(low) + " / " + Arrays.toString(high));
        } finally {
            ROLE.remove();
        }
    }
}
