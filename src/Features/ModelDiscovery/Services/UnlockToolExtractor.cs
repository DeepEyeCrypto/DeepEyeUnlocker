using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using DeepEyeUnlocker.Features.ModelDiscovery.Models;
using HtmlAgilityPack;

namespace DeepEyeUnlocker.Features.ModelDiscovery.Services
{
    public class UnlockToolExtractor : BaseExtractor
    {
        public override string ToolName => "UnlockTool";
        public override IEnumerable<string> SeedUrls => new[] 
        { 
            "https://edl.unlocktool.net/category/xiaomi",
            "https://file.unlocktool.net" 
        };

        public override async Task<IEnumerable<SupportedModel>> ExtractAsync(string url)
        {
            var html = await FetchHtmlAsync(url);
            var doc = new HtmlDocument();
            doc.LoadHtml(html);

            if (url.Contains("edl.unlocktool.net"))
            {
                return ParseEdlSite(doc, url);
            }
            return Enumerable.Empty<SupportedModel>();
        }

        private IEnumerable<SupportedModel> ParseEdlSite(HtmlDocument doc, string url)
        {
            var models = new List<SupportedModel>();
            // UnlockTool EDL site lists models in grid items or lists
            var nodes = doc.DocumentNode.SelectNodes("//a[contains(@class, 'model')]") ??
                        doc.DocumentNode.SelectNodes("//div[contains(@class, 'post-title')]");

            if (nodes == null) return models;

            foreach (var node in nodes)
            {
                string text = node.InnerText.Trim();
                string brand = url.Contains("xiaomi") ? "Xiaomi" : "Unknown";
                
                var modelInfo = new SupportedModel
                {
                    Tool = ToolName,
                    Brand = brand,
                    MarketingName = text,
                    ModelNumber = ParseModelNumber(text),
                    Codename = ParseCodename(text),
                    SourceUrl = url,
                    OperationsJson = "[\"FRP\", \"FactoryReset\", \"AuthBypass\"]",
                    ModesJson = "[\"EDL\"]"
                };

                models.Add(modelInfo);
            }
            return models;
        }
    }
}
