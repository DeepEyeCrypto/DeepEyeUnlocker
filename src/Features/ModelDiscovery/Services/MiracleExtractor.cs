using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using DeepEyeUnlocker.Features.ModelDiscovery.Models;
using HtmlAgilityPack;

namespace DeepEyeUnlocker.Features.ModelDiscovery.Services
{
    public class MiracleExtractor : BaseExtractor
    {
        public override string ToolName => "Miracle";
        public override IEnumerable<string> SeedUrls => new[] 
        { 
            "https://technicianhub.com/miracle-box-latest-setup/",
            "https://gsmserver.com/en/miracle-thunder-dongle/"
        };

        public override async Task<IEnumerable<SupportedModel>> ExtractAsync(string url)
        {
            var html = await FetchHtmlAsync(url);
            var doc = new HtmlDocument();
            doc.LoadHtml(html);

            var models = new List<SupportedModel>();
            // Miracle lists are often in pre/code blocks or plain text lists in technician hubs
            var nodes = doc.DocumentNode.SelectNodes("//pre") ?? doc.DocumentNode.SelectNodes("//code");

            if (nodes == null) return models;

            foreach (var node in nodes)
            {
                var lines = node.InnerText.Split(new[] { '\n', '\r' }, StringSplitOptions.RemoveEmptyEntries);
                foreach (var line in lines)
                {
                    var modelNum = ParseModelNumber(line);
                    if (modelNum != null)
                    {
                        models.Add(new SupportedModel
                        {
                            Tool = ToolName,
                            MarketingName = line.Trim(),
                            ModelNumber = modelNum,
                            SourceUrl = url,
                            OperationsJson = "[\"FRP\", \"WriteFirmware\", \"ReadPattern\"]"
                        });
                    }
                }
            }
            return models;
        }
    }
}
