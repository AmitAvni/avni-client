package com.avni.launcher.launch;

import com.avni.launcher.install.LaunchSpec;
import com.avni.launcher.model.Account;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Builds the JVM command line and spawns the Minecraft process. */
public class GameLauncher {

    public interface LineConsumer {
        void accept(String line);
    }

    public Process launch(LaunchSpec spec, Account account, int ramMb, LineConsumer log) throws IOException {
        String javaBin = spec.javaBin().toString();
        String classpath = spec.classpath().stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));

        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        cmd.add("-Xmx" + ramMb + "M");
        cmd.add("-Djava.library.path=" + spec.nativesDir());
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(spec.mainClass());

        // Minimal game arguments — username, uuid, token and userType for online play.
        cmd.add("--username");
        cmd.add(account.name());
        cmd.add("--version");
        cmd.add(spec.versionId());
        cmd.add("--gameDir");
        cmd.add(spec.gameDir().toString());
        cmd.add("--assetsDir");
        cmd.add(spec.assetsDir().toString());
        cmd.add("--assetIndex");
        cmd.add(spec.assetIndexId());
        cmd.add("--uuid");
        cmd.add(account.uuidNoDashes());
        cmd.add("--accessToken");
        cmd.add(account.accessToken());
        cmd.add("--userType");
        cmd.add("msa");
        cmd.add("--versionType");
        cmd.add("release");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(spec.gameDir().toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    log.accept(line);
                }
            } catch (IOException ignored) {
                // process ended
            }
        }, "mc-log");
        reader.setDaemon(true);
        reader.start();

        return process;
    }
}
