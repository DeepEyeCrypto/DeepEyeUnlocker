namespace DeepEyeUnlocker.Features.FrpBypass.Models
{
    public class FrpBrandProfile
    {
        [Newtonsoft.Json.JsonProperty("brand")]
        public string Brand { get; set; } = string.Empty;

        [Newtonsoft.Json.JsonProperty("os")]
        public string Os { get; set; } = string.Empty;

        [Newtonsoft.Json.JsonProperty("chipset_family")]
        public string ChipsetFamily { get; set; } = string.Empty;

        [Newtonsoft.Json.JsonProperty("method")]
        public string Method { get; set; } = string.Empty;

        [Newtonsoft.Json.JsonProperty("target_partition")]
        public string TargetPartition { get; set; } = string.Empty;

        [Newtonsoft.Json.JsonProperty("offset")]
        public long Offset { get; set; }

        [Newtonsoft.Json.JsonProperty("hex_payload")]
        public string HexPayload { get; set; } = string.Empty;

        [Newtonsoft.Json.JsonProperty("safety_check")]
        public string SafetyCheck { get; set; } = "verify_partition_backup_present";

        [Newtonsoft.Json.JsonProperty("detection_prop")]
        public string DetectionProp { get; set; } = string.Empty;

        [Newtonsoft.Json.JsonProperty("codename")]
        public string Codename { get; set; } = string.Empty;
    }
}
