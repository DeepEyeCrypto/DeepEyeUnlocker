using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using DeepEyeUnlocker.Features.ModelDiscovery.Models;
using HtmlAgilityPack;

namespace DeepEyeUnlocker.Features.ModelDiscovery.Services
{
    public class ChimeraExtractor : BaseExtractor
    {
        public override string ToolName => "Chimera";
        public override IEnumerable<string> SeedUrls => new[] 
        { 
            "https://chimeratool.com/en/models",
            "https://forum.gsmhosting.com/vbb/f898/chimera-tool-supported-models-list-2299691/"
        };

        public override async Task<IEnumerable<SupportedModel>> ExtractAsync(string url)
        {
            var html = await FetchHtmlAsync(url);
            var doc = new HtmlAgilityPack.HtmlDocument();
            doc.LoadHtml(html);

            if (url.Contains("forum.gsmhosting.com"))
            {
                return ParseForumList(doc, url);
            }
            else
            {
                return ParseOfficialSite(doc, url);
            }
        }

        private IEnumerable<SupportedModel> ParseForumList(HtmlAgilityPack.HtmlDocument doc, string url)
        {
            var models = new List<SupportedModel>();
            var postBody = doc.DocumentNode.SelectSingleNode("//div[contains(@id, 'post_message_')]");
            if (postBody == null) return models;

            // Forum posts are often long plaintext lists
            var lines = postBody.InnerText.Split(new[] { '\n', '\r' }, StringSplitOptions.RemoveEmptyEntries);
            string currentBrand = "Generic";

            foreach (var line in lines)
            {
                // Detect Brand Headings
                if (line.Contains("---") || line.Length < 2) continue;
                
                if (line.ToUpper().Contains("SAMSUNG") || line.ToUpper().Contains("XIAOMI"))
                {
                    currentBrand = NormalizeBrand(line);
                    continue;
                }

                var modelNum = ParseModelNumber(line);
                if (modelNum != null)
                {
                    models.Add(new SupportedModel
                    {
                        Tool = ToolName,
                        Brand = currentBrand,
                        MarketingName = line.Trim(),
                        ModelNumber = modelNum,
                        SourceUrl = url,
                        OperationsJson = "[\"FRP\", \"Flash\", \"NetworkUnlock\"]" // Generic for Chimera forum list
                    });
                }
            }

            return models;
        }

        private IEnumerable<SupportedModel> ParseOfficialSite(HtmlAgilityPack.HtmlDocument doc, string url)
        {
            var models = new List<SupportedModel>();
            // Chimera official site uses dynamic loading often, but we parse the static table if present
            var nodes = doc.DocumentNode.SelectNodes("//table//tr");
            if (nodes == null) return models;

            foreach (var node in nodes.Skip(1)) // Skip header
            {
                var cols = node.SelectNodes("td");
                if (cols != null && cols.Count >= 2)
                {
                    string brand = NormalizeBrand(cols[0].InnerText);
                    string modelText = cols[1].InnerText.Trim();
                    
                    models.Add(new SupportedModel
                    {
                        Tool = ToolName,
                        Brand = brand,
                        MarketingName = modelText,
                        ModelNumber = ParseModelNumber(modelText),
                        SourceUrl = url,
                        OperationsJson = "[\"FRP\", \"Flash\"]"
                    });
                }
            }
            return models;
        }
    }
}
