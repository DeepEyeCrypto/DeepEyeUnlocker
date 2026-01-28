namespace DeepEyeUnlocker.Core
{
    public class Device
    {
        public string Brand { get; set; } = "Unknown";
        public string Model { get; set; } = "Unknown";
        public string Chipset { get; set; } = "Unknown";
        public string Mode { get; set; } = "Unknown";
        public string SerialNumber { get; set; } = "";
        public string Imei { get; set; } = "";
        public string BootloaderStatus { get; set; } = "Unknown";
        public string AndroidVersion { get; set; } = "Unknown";

        public override string ToString() => $"{Brand} {Model} ({Mode})";
    }
}
