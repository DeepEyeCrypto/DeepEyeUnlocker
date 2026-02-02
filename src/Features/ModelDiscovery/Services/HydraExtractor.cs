using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using DeepEyeUnlocker.Features.ModelDiscovery.Models;
using HtmlAgilityPack;

namespace DeepEyeUnlocker.Features.ModelDiscovery.Services
{
    public class HydraExtractor : BaseExtractor
    {
        public override string ToolName => "Hydra";
        public override IEnumerable<string> SeedUrls => new[] 
        { 
            "https://www.hydradongle.com/devices",
            "https://www.hydradongle.com/download/software"
        };

        public override async Task<IEnumerable<SupportedModel>> ExtractAsync(string url)
        {
            var html = await FetchHtmlAsync(url);
            var doc = new HtmlDocument();
            doc.LoadHtml(html);

            var models = new List<SupportedModel>();
            // Hydra lists devices in brand-specific links or a central counter
            var nodes = doc.DocumentNode.SelectNodes("//div[contains(@class, 'device-item')]") ??
                        doc.DocumentNode.SelectNodes("//a[contains(@href, 'brand')]");

            if (nodes == null) return models;

            foreach (var node in nodes)
            {
                string text = node.InnerText.Trim();
                if (string.IsNullOrEmpty(text) || text.Length < 3) continue;

                models.Add(new SupportedModel
                {
                    Tool = ToolName,
                    MarketingName = text,
                    ModelNumber = ParseModelNumber(text),
                    SourceUrl = url,
                    OperationsJson = "[\"FRP\", \"RepairIMEI\", \"FormatCustom\"]"
                });
            }
            return models;
        }
    }
}
