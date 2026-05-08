package tn.esprit.mythoria.utils;

import javafx.scene.image.Image;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class LocalImageUtil {
    private LocalImageUtil() {
    }

    public static Image loadImage(String imageSource, double requestedWidth, double requestedHeight) {
        if (imageSource == null || imageSource.isBlank()) {
            return null;
        }

        for (String candidate : buildCandidates(imageSource.trim())) {
            try {
                Image image = new Image(candidate, requestedWidth, requestedHeight, true, true, true);
                if (!image.isError()) {
                    return image;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }

    private static List<String> buildCandidates(String imageSource) {
        List<String> candidates = new ArrayList<>();

        if (imageSource.startsWith("http://")
                || imageSource.startsWith("https://")
                || imageSource.startsWith("file:/")) {
            candidates.add(imageSource);
            return candidates;
        }

        File directFile = new File(imageSource);
        if (directFile.exists()) {
            candidates.add(directFile.toURI().toString());
        }

        File resourceFile = new File("src/main/resources", imageSource);
        if (resourceFile.exists()) {
            candidates.add(resourceFile.toURI().toString());
        }

        addClasspathCandidate(candidates, imageSource);
        addClasspathCandidate(candidates, "/" + imageSource);
        addClasspathCandidate(candidates, "/tn/esprit/mythoria/" + imageSource);

        return candidates;
    }

    private static void addClasspathCandidate(List<String> candidates, String resourcePath) {
        URL url = LocalImageUtil.class.getResource(resourcePath);
        if (url != null) {
            candidates.add(url.toExternalForm());
        }
    }
}
