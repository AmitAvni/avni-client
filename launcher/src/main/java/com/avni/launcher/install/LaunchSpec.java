package com.avni.launcher.install;

import java.nio.file.Path;
import java.util.List;

/** Everything the {@code GameLauncher} needs to build the launch command. */
public record LaunchSpec(
        String versionId,
        List<Path> classpath,
        String mainClass,
        Path nativesDir,
        Path assetsDir,
        String assetIndexId,
        Path gameDir,
        Path javaBin) {
}
