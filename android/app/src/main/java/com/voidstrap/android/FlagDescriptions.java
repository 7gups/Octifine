package com.voidstrap.android;

import java.util.HashMap;
import java.util.Map;

/**
 * Descriptions for flags that exist in the 2021 build. The text is paraphrased from a community flag list
 * (GamingBoyZ379/robloxfastflags) and has not been checked against the 2021 client. Flags not listed here
 * get a description generated from their name.
 */
final class FlagDescriptions {
    private static final Map<String, String> TEXT = new HashMap<>();

    static {
        TEXT.put("DFIntCanHideGuiGroupId", "Group ID for the GUI hiding feature. Set it to a group you are in.");
        TEXT.put("FIntRenderShadowIntensity", "Set to 0 to disable player shadows.");
        TEXT.put("FIntRenderShadowmapBias", "Shadow map bias. Applies to future and shadow map lighting only.");
        TEXT.put("DFFlagDebugPauseVoxelizer", "Pauses the voxelizer. Also disables baked shadows.");
        TEXT.put("DFFlagDebugRenderForceTechnologyVoxel", "Forces voxel lighting (phase 1).");
        TEXT.put("DFFlagDisableDPIScale", "Keeps rendering quality when the display uses a scale setting.");
        TEXT.put("FIntFRMMinGrassDistance", "Minimum grass distance. Set this and the maximum to 0 to remove grass.");
        TEXT.put("FIntFRMMaxGrassDistance", "Maximum grass distance. Set this and the minimum to 0 to remove grass.");
        TEXT.put("FIntRenderGrassDetailStrands", "Grass strand detail. 0 removes grass strands.");
        TEXT.put("DFFlagDebugDisableTimeoutDisconnect", "Hides the timeout message. You are still disconnected, but the message is not shown.");
        TEXT.put("DFIntDefaultTimeoutTimeMs", "Default timeout in milliseconds. 1000 is one second.");
        TEXT.put("DFIntConnectionMTUSize", "Network MTU. Find the largest packet size that does not fragment, then add 28 bytes.");
        TEXT.put("DFIntCSGLevelOfDetailSwitchingDistance", "Distance at which mesh level of detail switches. 0 forces LOD on meshes.");
        TEXT.put("DFIntCSGLevelOfDetailSwitchingDistanceL12", "Level of detail switch distance from level 1 to 2. 0 forces LOD on meshes.");
        TEXT.put("DFIntMaxFrameBufferSize", "Frame buffer size. 4 is stable. 0 can show a white screen, and 1 to 3 can make other players move laggy.");
        TEXT.put("DFIntMaxActiveAnimationTracks", "Limits active animation tracks. 0 removes most player animations.");
        TEXT.put("FFlagHandleAltEnterFullscreenManually", "Set to false for exclusive fullscreen. The community guide uses Alt+Delete to toggle it.");
        TEXT.put("DFFlagDebugDrawBvhNodes", "Draws an outline around every body part.");
        TEXT.put("FFlagDebugHumanoidRendering", "Highlights parts the player touches and draws a blue circle under the character.");
        TEXT.put("FFlagDebugDisplayFPS", "Shows the frame rate on screen.");
    }

    private FlagDescriptions() {}

    /** The community description, or null when there is none for this flag. */
    static String get(String key) {
        return TEXT.get(key);
    }
}
