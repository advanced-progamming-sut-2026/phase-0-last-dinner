package college.java.project.graphics;

import com.badlogic.gdx.files.FileHandle;
import pvz.libpvz.textures.ResourceIndex;
import pvz.libpvz.textures.TextureBank;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs a fail-closed preflight for PAM texture dependencies.
 *
 * libPVZ can parse/bake a PAM and return bounds even when one of the texture atlases
 * used by the baked parts is absent. Rendering then reaches a part with no UV and
 * fails. This validator checks the image resource ids embedded in the PAM against
 * the ResourceIndex and verifies that every referenced atlas file actually exists.
 */
final class PamTextureAvailability {
    private static final Pattern IMAGE_RESOURCE_PATTERN =
            Pattern.compile("IMAGE_[A-Za-z0-9_]+");
    private static final int MAX_BINARY_SUFFIX = 1;

    private PamTextureAvailability() {
    }

    static boolean allTexturesAvailable(TextureBank textureBank, FileHandle pamFile) {
        if (textureBank == null || pamFile == null || !pamFile.exists()) {
            return false;
        }
        FileHandle runtimePam = new FileHandle(pamFile.path());
        if (!runtimePam.exists()) {
            return false;
        }

        ResourceIndex resourceIndex = textureBank.getResourceIndex();
        if (resourceIndex == null) {
            return false;
        }

        final String binaryText;
        try {
            binaryText = new String(pamFile.readBytes(), StandardCharsets.ISO_8859_1);
        } catch (RuntimeException exception) {
            return false;
        }

        Matcher matcher = IMAGE_RESOURCE_PATTERN.matcher(binaryText);
        Set<String> checkedAtlases = new HashSet<>();
        boolean foundImageResource = false;

        while (matcher.find()) {
            ResourceIndex.ImageEntry image = resolveImage(resourceIndex, matcher.group());
            if (image == null || image.atlasId == null || image.atlasId.trim().isEmpty()) {
                return false;
            }

            foundImageResource = true;
            if (!checkedAtlases.add(image.atlasId)) {
                continue;
            }

            FileHandle atlasFile = textureBank.atlasFile(image.atlasId);
            if (atlasFile == null || !atlasFile.exists()) {
                return false;
            }
        }

        // Plant preview PAMs are expected to contain image resources. A PAM for which
        // no texture reference can be proven is not safe enough to hand to the actor.
        return foundImageResource;
    }

    private static ResourceIndex.ImageEntry resolveImage(
            ResourceIndex resourceIndex,
            String binaryCandidate
    ) {
        ResourceIndex.ImageEntry exact = resourceIndex.image(binaryCandidate);
        if (exact != null) {
            return exact;
        }

        // PAM string fields are length-prefixed binary data. The regex can occasionally
        // consume the following single printable metadata byte (for example ..._82X74R).
        // Only that one-byte suffix is tolerated; anything longer fails closed rather
        // than accidentally resolving an unrelated shorter resource id.
        if (binaryCandidate.length() <= "IMAGE_".length()) {
            return null;
        }

        String trimmed = binaryCandidate.substring(0, binaryCandidate.length() - 1);
        ResourceIndex.ImageEntry trimmedEntry = resourceIndex.image(trimmed);
        if (trimmedEntry != null
                && binaryCandidate.length() - trimmed.length() <= MAX_BINARY_SUFFIX) {
            return trimmedEntry;
        }
        return null;
    }
}
