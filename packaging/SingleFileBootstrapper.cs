using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;

namespace InstintoSelvagem
{
    static class Program
    {
        private const string AppDirName = "InstintoSelvagem";
        private const string SubDirName = "game_bundle";
        private const string ExeName = "InstintoSelvagem.exe";
        private const string VersionMarker = "1.0.1";

        [STAThread]
        static int Main(string[] args)
        {
            try
            {
                string localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
                string baseDir = Path.Combine(localAppData, AppDirName);
                string gameDir = Path.Combine(baseDir, SubDirName);
                string targetExe = Path.Combine(gameDir, ExeName);
                string markerFile = Path.Combine(gameDir, ".version");

                bool needsExtract = !File.Exists(targetExe) ||
                                    !File.Exists(markerFile) ||
                                    File.ReadAllText(markerFile).Trim() != VersionMarker;

                if (needsExtract)
                {
                    if (Directory.Exists(gameDir))
                    {
                        try { Directory.Delete(gameDir, true); } catch { }
                    }
                    Directory.CreateDirectory(gameDir);

                    Assembly asm = Assembly.GetExecutingAssembly();
                    using (Stream zipStream = asm.GetManifestResourceStream("game_payload.zip"))
                    {
                        if (zipStream == null)
                        {
                            return 1;
                        }

                        using (ZipArchive archive = new ZipArchive(zipStream, ZipArchiveMode.Read))
                        {
                            foreach (ZipArchiveEntry entry in archive.Entries)
                            {
                                string relativePath = entry.FullName;
                                if (relativePath.StartsWith("InstintoSelvagem/", StringComparison.OrdinalIgnoreCase) ||
                                    relativePath.StartsWith("InstintoSelvagem\\", StringComparison.OrdinalIgnoreCase))
                                {
                                    relativePath = relativePath.Substring(17);
                                }
                                if (string.IsNullOrEmpty(relativePath)) continue;

                                string destPath = Path.Combine(gameDir, relativePath.Replace('/', Path.DirectorySeparatorChar));
                                if (entry.FullName.EndsWith("/") || entry.FullName.EndsWith("\\"))
                                {
                                    Directory.CreateDirectory(destPath);
                                }
                                else
                                {
                                    string parent = Path.GetDirectoryName(destPath);
                                    if (!string.IsNullOrEmpty(parent) && !Directory.Exists(parent))
                                    {
                                        Directory.CreateDirectory(parent);
                                    }
                                    entry.ExtractToFile(destPath, true);
                                }
                            }
                        }
                    }

                    File.WriteAllText(markerFile, VersionMarker);
                }

                if (!File.Exists(targetExe))
                {
                    return 2;
                }

                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = targetExe;
                psi.WorkingDirectory = gameDir;
                psi.UseShellExecute = false;
                if (args != null && args.Length > 0)
                {
                    psi.Arguments = string.Join(" ", args);
                }

                Process.Start(psi);
                return 0;
            }
            catch (Exception ex)
            {
                try
                {
                    string errFile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), AppDirName, "launcher_error.log");
                    File.WriteAllText(errFile, ex.ToString());
                }
                catch { }
                return -1;
            }
        }
    }
}

