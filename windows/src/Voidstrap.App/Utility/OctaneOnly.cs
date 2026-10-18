using System;
using System.IO;

namespace Voidstrap.Utility;

// Locks the Windows player to the local Octane 2021 build.
// When Enabled, Voidstrap never fetches, downloads, or upgrades Roblox builds.
public static class OctaneOnly
{
    public const bool Enabled = true;

    public const string ExecutableName = "OctanePlayer.exe";

    // Matches the version-<16 hex> format so the bootstrapper never resets it.
    public const string MarkerVersionGuid = "version-0000000000000000";

    public static string ClientDirectory => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "Octane", "clients", "2021");

    public static string ExecutablePath => Path.Combine(ClientDirectory, ExecutableName);
}
