package lancement;

import examen.Exam;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestsExamen {
    private TestsExamen() {}

    public static void main(String[] args) throws IOException {
        Path logPath = Path.of(args[0]).toAbsolutePath();
        PrintStream console = System.err;

        try (var stream = new BufferedOutputStream(Files.newOutputStream(logPath));
             var log = new PrintStream(stream, false, StandardCharsets.UTF_8)) {
            System.setErr(log);
            try {
                Exam.main(new String[]{"--woven"});
            } finally {
                System.setErr(console);
                System.out.println("Journal AspectJ : " + logPath);
            }
            if (log.checkError()) {
                throw new IOException("Échec d'écriture du journal : " + logPath);
            }
        }
    }
}
