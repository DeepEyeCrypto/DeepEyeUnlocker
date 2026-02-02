using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using DeepEyeUnlocker.Features.ModelDiscovery.Models;
using HtmlAgilityPack;

namespace DeepEyeUnlocker.Features.ModelDiscovery.Services
{
    public abstract class BaseExtractor : ISourceExtractor
    {
        public abstract string ToolName { get; }
        public abstract IEnumerable<string> SeedUrls { get; }

        protected readonly HttpClient _client = new();

        public abstract Task<IEnumerable<SupportedModel>> ExtractAsync(string url);

        protected async Task<string> FetchHtmlAsync(string url)
        {
            _client.DefaultRequestHeaders.UserAgent.ParseAdd("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            return await _client.GetStringAsync(url);
        }

        protected string? ParseModelNumber(string text)
        {
            // Patterns: SM-S928B, RMX3085, CPH2219, CP9863A, etc.
            var match = Regex.Match(text, @"(SM-[A-Z0-9]+|RMX[0-9]+|CPH[0-9]+|XT[0-9]{4}|LE[0-9]{4})", RegexOptions.IgnoreCase);
            return match.Success ? match.Value.ToUpper() : null;
        }

        protected string? ParseCodename(string text)
        {
            // Pattern: (codename)
            var match = Regex.Match(text, @"\(([^)]+)\)");
            return match.Success ? match.Groups[1].Value.ToLower() : null;
        }

        protected string NormalizeBrand(string brand)
        {
            brand = brand.Trim().ToUpper();
            if (brand.Contains("SAMSUNG")) return "Samsung";
            if (brand.Contains("XIAOMI")) return "Xiaomi";
            if (brand.Contains("REDMI")) return "Redmi";
            if (brand.Contains("POCO")) return "POCO";
            if (brand.Contains("VIVO")) return "Vivo";
            if (brand.Contains("OPPO")) return "Oppo";
            if (brand.Contains("REALME")) return "Realme";
            if (brand.Contains("TECNO")) return "Tecno";
            if (brand.Contains("INFINIX")) return "Infinix";
            if (brand.Contains("HUAWEI")) return "Huawei";
            return brand;
        }
    }
}
